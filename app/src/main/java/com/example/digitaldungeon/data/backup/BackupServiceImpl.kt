package com.example.digitaldungeon.data.backup

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.CampaignParticipantDao
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.decodeParticipants
import com.example.digitaldungeon.data.mapper.encodeParticipants
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject

/**
 * Pretty printed on purpose: the point of a document backup over a file copy is that a human can
 * open it. Unknown keys are ignored so a file written by a newer build of the *same* format
 * version still restores, and defaults are always encoded so [BackupSnapshot.formatVersion] can
 * never be missing from a file this app wrote.
 */
private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Read before the document is trusted enough to be deserialized as a whole. */
private const val FIELD_FORMAT_VERSION = "formatVersion"

class BackupServiceImpl @Inject constructor(
    private val database: AppDatabase,
    private val campaignDao: CampaignDao,
    private val participantDao: CampaignParticipantDao,
    private val sessionDao: SessionDao,
    private val eventDao: SessionEventDao
) : BackupService {

    override suspend fun export(): String {

        // Four reads that have to agree with each other, so they are taken as one snapshot:
        // a write landing between them would produce a session without its campaign.
        val snapshot = database.withTransaction {

            val participantsByCampaign = participantDao
                .getAllParticipants()
                .groupBy { it.campaignId }

            val sessionsByCampaign = sessionDao
                .getAllSessions()
                .groupBy { it.campaignId }

            val eventsBySession = eventDao
                .getAllEvents()
                .groupBy { it.sessionId }

            BackupSnapshot(
                formatVersion = BACKUP_FORMAT_VERSION,
                createdAt = System.currentTimeMillis(),
                campaigns = campaignDao.getAllCampaigns().map { campaign ->

                    campaign.toBackup(
                        participants = participantsByCampaign[campaign.id].orEmpty(),
                        sessions = sessionsByCampaign[campaign.id]
                            .orEmpty()
                            .map { session ->
                                session.toBackup(eventsBySession[session.id].orEmpty())
                            }
                    )
                }
            )
        }

        return backupJson.encodeToString(snapshot)
    }

    override suspend fun restore(json: String): RestoreResult {

        // The version is read off the raw tree first. Deserializing the whole document straight
        // away would report a future format as unreadable JSON instead of as a future format.
        val tree = try {
            backupJson.parseToJsonElement(json)
        } catch (e: IllegalArgumentException) {
            return RestoreResult.InvalidJson
        }

        val formatVersion = (tree as? JsonObject)
            ?.get(FIELD_FORMAT_VERSION)
            ?.let { it as? JsonPrimitive }
            ?.intOrNull
            ?: return RestoreResult.InvalidJson

        if (formatVersion != BACKUP_FORMAT_VERSION) {
            return RestoreResult.UnsupportedFormatVersion(formatVersion)
        }

        val snapshot = try {
            backupJson.decodeFromJsonElement<BackupSnapshot>(tree)
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException: a missing required field or
            // a wrong type lands here just like malformed syntax does.
            return RestoreResult.InvalidJson
        }

        if (!snapshot.hasRestorableIds()) {
            return RestoreResult.InvalidJson
        }

        // Everything is turned into rows before the transaction opens, so the transaction holds
        // the database for inserts only and can never fail halfway through mapping.
        val campaigns = mutableListOf<CampaignEntity>()
        val participants = mutableListOf<CampaignParticipantEntity>()
        val sessions = mutableListOf<SessionEntity>()
        val events = mutableListOf<SessionEventEntity>()

        snapshot.campaigns.forEach { campaign ->

            campaigns += campaign.toEntity()

            campaign.participants.forEach { participant ->
                participants += participant.toEntity(campaignId = campaign.id)
            }

            campaign.sessions.forEach { session ->

                sessions += session.toEntity(campaignId = campaign.id)

                session.events.forEach { event ->
                    events += event.toEntity(sessionId = session.id)
                }
            }
        }

        database.withTransaction {

            // Children first on the way out, parents first on the way in: both directions have to
            // respect the foreign keys even though deleting a campaign would cascade anyway.
            eventDao.deleteAllEvents()
            sessionDao.deleteAllSessions()
            participantDao.deleteAllParticipants()
            campaignDao.deleteAllCampaigns()

            campaignDao.insertCampaigns(campaigns)
            participantDao.insertParticipants(participants)
            sessionDao.insertSessions(sessions)
            eventDao.insertEvents(events)
        }

        return RestoreResult.Success(
            campaigns = campaigns.size,
            campaignParticipants = participants.size,
            sessions = sessions.size,
            sessionEvents = events.size
        )
    }
}

/**
 * Whether every id in the document can be written back as it stands.
 *
 * Room reads `id = 0` on an autogenerated key as "assign one", which would quietly detach children
 * from the parent they are nested under, and a duplicate id would abort the insert. Either way the
 * document is not something this app ever wrote, so it is refused before anything is deleted.
 */
private fun BackupSnapshot.hasRestorableIds(): Boolean {

    val campaignIds = mutableSetOf<Int>()
    val participantIds = mutableSetOf<Int>()
    val sessionIds = mutableSetOf<Int>()
    val eventIds = mutableSetOf<Int>()

    campaigns.forEach { campaign ->

        if (!campaignIds.addRestorable(campaign.id)) return false

        campaign.participants.forEach { participant ->
            if (!participantIds.addRestorable(participant.id)) return false
        }

        campaign.sessions.forEach { session ->

            if (!sessionIds.addRestorable(session.id)) return false

            session.events.forEach { event ->
                if (!eventIds.addRestorable(event.id)) return false
            }
        }
    }

    return true
}

private fun MutableSet<Int>.addRestorable(id: Int): Boolean =
    id > 0 && add(id)

private fun CampaignEntity.toBackup(
    participants: List<CampaignParticipantEntity>,
    sessions: List<BackupSession>
): BackupCampaign =
    BackupCampaign(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = participants.map { it.toBackup() },
        sessions = sessions
    )

private fun CampaignParticipantEntity.toBackup(): BackupCampaignParticipant =
    BackupCampaignParticipant(
        id = id,
        name = name,
        level = level,
        maxHp = maxHp,
        orderIndex = orderIndex
    )

private fun SessionEntity.toBackup(
    events: List<SessionEventEntity>
): BackupSession =
    BackupSession(
        id = id,
        title = title,
        notes = notes,
        createdAt = createdAt,
        events = events.map { it.toBackup() }
    )

private fun SessionEventEntity.toBackup(): BackupSessionEvent =
    BackupSessionEvent(
        id = id,
        type = type.name,
        title = title,
        content = content,
        orderIndex = orderIndex,
        timestamp = timestamp,
        // The stored column is already JSON, but it is unfolded into real objects instead of being
        // smuggled through as an escaped string: a backup is supposed to be readable.
        participants = decodeParticipants(participants).map { it.toBackup() }
    )

private fun EventParticipant.toBackup(): BackupEventParticipant =
    BackupEventParticipant(
        name = name,
        level = level,
        maxHp = maxHp
    )

private fun BackupCampaign.toEntity(): CampaignEntity =
    CampaignEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )

private fun BackupCampaignParticipant.toEntity(campaignId: Int): CampaignParticipantEntity =
    CampaignParticipantEntity(
        id = id,
        campaignId = campaignId,
        name = name,
        level = level,
        maxHp = maxHp,
        orderIndex = orderIndex
    )

private fun BackupSession.toEntity(campaignId: Int): SessionEntity =
    SessionEntity(
        id = id,
        campaignId = campaignId,
        title = title,
        notes = notes,
        createdAt = createdAt
    )

private fun BackupSessionEvent.toEntity(sessionId: Int): SessionEventEntity =
    SessionEventEntity(
        id = id,
        sessionId = sessionId,
        type = type.toSessionEventType(),
        title = title,
        content = content,
        orderIndex = orderIndex,
        timestamp = timestamp,
        participants = encodeParticipants(participants.map { it.toDomain() })
    )

private fun BackupEventParticipant.toDomain(): EventParticipant =
    EventParticipant(
        name = name,
        level = level,
        maxHp = maxHp
    )

/**
 * A type name this build does not know - a file from a version that had more types - degrades to
 * [SessionEventType.NOTE] rather than failing the restore: what the user wrote is worth more than
 * how it was categorised.
 */
private fun String.toSessionEventType(): SessionEventType =
    SessionEventType.entries.firstOrNull { it.name == this } ?: SessionEventType.NOTE
