package com.example.digitaldungeon.data.backup

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.CampaignParticipantDao
import com.example.digitaldungeon.data.local.dao.QuestConsequenceDao
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.local.dao.QuestNoteDao
import com.example.digitaldungeon.data.local.dao.QuestObjectiveDao
import com.example.digitaldungeon.data.local.dao.QuestRelationDao
import com.example.digitaldungeon.data.local.dao.QuestRewardDao
import com.example.digitaldungeon.data.local.dao.QuestSessionLinkDao
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.QuestConsequenceEntity
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import com.example.digitaldungeon.data.local.entity.QuestNoteEntity
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.data.local.entity.QuestRelationEntity
import com.example.digitaldungeon.data.local.entity.QuestRewardEntity
import com.example.digitaldungeon.data.local.entity.QuestSessionLinkEntity
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.decodeParticipants
import com.example.digitaldungeon.data.mapper.encodeParticipants
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestRelationType
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestStatus
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
    private val eventDao: SessionEventDao,
    private val questDao: QuestDao,
    private val objectiveDao: QuestObjectiveDao,
    private val rewardDao: QuestRewardDao,
    private val consequenceDao: QuestConsequenceDao,
    private val noteDao: QuestNoteDao,
    private val relationDao: QuestRelationDao,
    private val historyDao: QuestHistoryDao,
    private val sessionLinkDao: QuestSessionLinkDao
) : BackupService {

    override suspend fun export(): String {

        // Twelve reads that have to agree with each other, so they are taken as one snapshot:
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

            val allQuests = questDao.getAllQuests()

            val questsByCampaign = allQuests.groupBy { it.campaignId }

            val objectivesByQuest = objectiveDao
                .getAllObjectives()
                .groupBy { it.questId }

            val rewardsByQuest = rewardDao
                .getAllRewards()
                .groupBy { it.questId }

            val consequencesByQuest = consequenceDao
                .getAllConsequences()
                .groupBy { it.questId }

            val notesByQuest = noteDao
                .getAllNotes()
                .groupBy { it.questId }

            // Grouped by the campaign of the source quest. Both ends are in the same campaign by
            // the rule the repository enforces, so either end would do; the source is the one the
            // document nests the row under.
            val campaignOfQuest = allQuests.associate { it.id to it.campaignId }

            val relationsByCampaign = relationDao
                .getAllRelations()
                .groupBy { campaignOfQuest[it.sourceQuestId] }

            val historyByQuest = historyDao
                .getAllEntries()
                .groupBy { it.questId }

            // Grouped by the campaign of the quest, which is the end that is always in this
            // campaign; the session is in the same one by the rule the repository enforces.
            val sessionLinksByCampaign = sessionLinkDao
                .getAllLinks()
                .groupBy { campaignOfQuest[it.questId] }

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
                            },
                        questRelations = relationsByCampaign[campaign.id]
                            .orEmpty()
                            .map { it.toBackup() },
                        questSessionLinks = sessionLinksByCampaign[campaign.id]
                            .orEmpty()
                            .map { it.toBackup() },
                        quests = questsByCampaign[campaign.id]
                            .orEmpty()
                            .map { quest ->
                                quest.toBackup(
                                    objectives = objectivesByQuest[quest.id].orEmpty(),
                                    rewards = rewardsByQuest[quest.id].orEmpty(),
                                    consequences = consequencesByQuest[quest.id].orEmpty(),
                                    notes = notesByQuest[quest.id].orEmpty(),
                                    history = historyByQuest[quest.id].orEmpty()
                                )
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

        // Older formats are read, newer ones are refused: a version 1 file carries no quests and
        // restores as it always did, while a file from a future version would have to be restored
        // by dropping the fields this build cannot see - which is worse than not restoring it.
        if (formatVersion !in SUPPORTED_BACKUP_FORMAT_VERSIONS) {
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
        val quests = mutableListOf<QuestEntity>()
        val objectives = mutableListOf<QuestObjectiveEntity>()
        val rewards = mutableListOf<QuestRewardEntity>()
        val consequences = mutableListOf<QuestConsequenceEntity>()
        val notes = mutableListOf<QuestNoteEntity>()
        val relations = mutableListOf<QuestRelationEntity>()
        val history = mutableListOf<QuestHistoryEntity>()
        val sessionLinks = mutableListOf<QuestSessionLinkEntity>()

        snapshot.campaigns.forEach { campaign ->

            campaigns += campaign.toEntity()

            campaign.participants.forEach { participant ->
                participants += participant.toEntity(campaignId = campaign.id)
            }

            campaign.questRelations.forEach { relation ->
                relations += relation.toEntity()
            }

            campaign.questSessionLinks.forEach { link ->
                sessionLinks += link.toEntity()
            }

            campaign.quests.forEach { quest ->

                quests += quest.toEntity(campaignId = campaign.id)

                quest.objectives.forEach { objective ->
                    objectives += objective.toEntity(questId = quest.id)
                }

                quest.rewards.forEach { reward ->
                    rewards += reward.toEntity(questId = quest.id)
                }

                quest.consequences.forEach { consequence ->
                    consequences += consequence.toEntity(questId = quest.id)
                }

                quest.notes.forEach { note ->
                    notes += note.toEntity(questId = quest.id)
                }

                quest.history.forEach { entry ->
                    history += entry.toEntity(questId = quest.id)
                }
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
            relationDao.deleteAllRelations()
            historyDao.deleteAllEntries()
            sessionLinkDao.deleteAllLinks()
            objectiveDao.deleteAllObjectives()
            rewardDao.deleteAllRewards()
            consequenceDao.deleteAllConsequences()
            noteDao.deleteAllNotes()
            questDao.deleteAllQuests()
            participantDao.deleteAllParticipants()
            campaignDao.deleteAllCampaigns()

            campaignDao.insertCampaigns(campaigns)
            participantDao.insertParticipants(participants)
            questDao.insertQuests(quests)
            objectiveDao.insertObjectives(objectives)
            rewardDao.insertRewards(rewards)
            consequenceDao.insertConsequences(consequences)
            noteDao.insertNotes(notes)
            // After the quests, because both ends are foreign keys into them.
            relationDao.insertRelations(relations)
            historyDao.insertEntries(history)
            sessionDao.insertSessions(sessions)
            eventDao.insertEvents(events)
            // Last of all: this is the only table with a foreign key into both the quests and the
            // sessions, so it cannot be written until both are back.
            sessionLinkDao.insertLinks(sessionLinks)
        }

        return RestoreResult.Success(
            campaigns = campaigns.size,
            campaignParticipants = participants.size,
            sessions = sessions.size,
            sessionEvents = events.size,
            quests = quests.size,
            questObjectives = objectives.size,
            questRewards = rewards.size,
            questConsequences = consequences.size,
            questNotes = notes.size,
            questRelations = relations.size,
            questHistoryEntries = history.size,
            questSessionLinks = sessionLinks.size
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
    val questIds = mutableSetOf<Int>()
    val objectiveIds = mutableSetOf<Int>()
    val rewardIds = mutableSetOf<Int>()
    val consequenceIds = mutableSetOf<Int>()
    val noteIds = mutableSetOf<Int>()
    val relationIds = mutableSetOf<Int>()
    val historyIds = mutableSetOf<Int>()
    val sessionLinkIds = mutableSetOf<Int>()

    campaigns.forEach { campaign ->

        if (!campaignIds.addRestorable(campaign.id)) return false

        campaign.participants.forEach { participant ->
            if (!participantIds.addRestorable(participant.id)) return false
        }

        campaign.questRelations.forEach { relation ->
            if (!relationIds.addRestorable(relation.id)) return false
        }

        campaign.questSessionLinks.forEach { link ->
            if (!sessionLinkIds.addRestorable(link.id)) return false
        }

        campaign.quests.forEach { quest ->

            if (!questIds.addRestorable(quest.id)) return false

            quest.objectives.forEach { objective ->
                if (!objectiveIds.addRestorable(objective.id)) return false
            }

            quest.rewards.forEach { reward ->
                if (!rewardIds.addRestorable(reward.id)) return false
            }

            quest.consequences.forEach { consequence ->
                if (!consequenceIds.addRestorable(consequence.id)) return false
            }

            quest.notes.forEach { note ->
                if (!noteIds.addRestorable(note.id)) return false
            }

            quest.history.forEach { entry ->
                if (!historyIds.addRestorable(entry.id)) return false
            }
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
    sessions: List<BackupSession>,
    quests: List<BackupQuest>,
    questRelations: List<BackupQuestRelation>,
    questSessionLinks: List<BackupQuestSessionLink>
): BackupCampaign =
    BackupCampaign(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = participants.map { it.toBackup() },
        sessions = sessions,
        questRelations = questRelations,
        questSessionLinks = questSessionLinks,
        quests = quests
    )

private fun CampaignParticipantEntity.toBackup(): BackupCampaignParticipant =
    BackupCampaignParticipant(
        id = id,
        name = name,
        level = level,
        maxHp = maxHp,
        orderIndex = orderIndex
    )

private fun QuestEntity.toBackup(
    objectives: List<QuestObjectiveEntity>,
    rewards: List<QuestRewardEntity>,
    consequences: List<QuestConsequenceEntity>,
    notes: List<QuestNoteEntity>,
    history: List<QuestHistoryEntity>
): BackupQuest =
    BackupQuest(
        id = id,
        title = title,
        description = description,
        category = category.name,
        status = status.name,
        priority = priority.name,
        isHidden = isHidden,
        createdAt = createdAt,
        inGameDate = inGameDate,
        objectives = objectives.map { it.toBackup() },
        rewards = rewards.map { it.toBackup() },
        consequences = consequences.map { it.toBackup() },
        notes = notes.map { it.toBackup() },
        history = history.map { it.toBackup() }
    )

private fun QuestHistoryEntity.toBackup(): BackupQuestHistoryEntry =
    BackupQuestHistoryEntry(
        id = id,
        eventType = eventType.name,
        description = description,
        timestamp = timestamp
    )

private fun BackupQuestHistoryEntry.toEntity(questId: Int): QuestHistoryEntity =
    QuestHistoryEntity(
        id = id,
        questId = questId,
        eventType = eventType.toQuestHistoryEventType(),
        description = description,
        // Carried, never re-stamped: an entry says when the thing happened, and a restore is not
        // the thing happening again.
        timestamp = timestamp
    )

/**
 * An unknown history event degrades to [QuestHistoryEventType.QUEST_STATUS_CHANGED].
 *
 * The least wrong reading available: every entry this app writes is about a status moving, and the
 * description is carried verbatim either way, so the line still says when something happened to
 * the quest even if this build cannot name what.
 */
private fun String.toQuestHistoryEventType(): QuestHistoryEventType =
    QuestHistoryEventType.entries.firstOrNull { it.name == this }
        ?: QuestHistoryEventType.QUEST_STATUS_CHANGED

private fun QuestSessionLinkEntity.toBackup(): BackupQuestSessionLink =
    BackupQuestSessionLink(
        id = id,
        questId = questId,
        sessionId = sessionId
    )

private fun BackupQuestSessionLink.toEntity(): QuestSessionLinkEntity =
    QuestSessionLinkEntity(
        id = id,
        questId = questId,
        sessionId = sessionId
    )

private fun QuestRelationEntity.toBackup(): BackupQuestRelation =
    BackupQuestRelation(
        id = id,
        sourceQuestId = sourceQuestId,
        targetQuestId = targetQuestId,
        relationType = relationType.name
    )

private fun BackupQuestRelation.toEntity(): QuestRelationEntity =
    QuestRelationEntity(
        id = id,
        // The ids are the document's own, carried verbatim like every other key here, so the row
        // keeps pointing at the same two quests it did before the export.
        sourceQuestId = sourceQuestId,
        targetQuestId = targetQuestId,
        relationType = relationType.toQuestRelationType()
    )

/**
 * An unknown relation type degrades to [QuestRelationType.RELATED].
 *
 * The weakest claim the model can make: it says the two quests have something to do with each
 * other, which is true of any link, without inventing a direction or a hierarchy that this build
 * cannot read.
 */
private fun String.toQuestRelationType(): QuestRelationType =
    QuestRelationType.entries.firstOrNull { it.name == this } ?: QuestRelationType.RELATED

private fun QuestRewardEntity.toBackup(): BackupQuestReward =
    BackupQuestReward(
        id = id,
        title = title,
        description = description,
        type = type.name
    )

private fun QuestConsequenceEntity.toBackup(): BackupQuestConsequence =
    BackupQuestConsequence(
        id = id,
        title = title,
        description = description,
        type = type.name
    )

private fun QuestNoteEntity.toBackup(): BackupQuestNote =
    BackupQuestNote(
        id = id,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun QuestObjectiveEntity.toBackup(): BackupQuestObjective =
    BackupQuestObjective(
        id = id,
        title = title,
        description = description,
        status = status.name,
        orderIndex = orderIndex,
        isOptional = isOptional
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

private fun BackupQuest.toEntity(campaignId: Int): QuestEntity =
    QuestEntity(
        id = id,
        campaignId = campaignId,
        title = title,
        description = description,
        category = category.toQuestCategory(),
        status = status.toQuestStatus(),
        priority = priority.toQuestPriority(),
        isHidden = isHidden,
        createdAt = createdAt,
        inGameDate = inGameDate
    )

private fun BackupQuestObjective.toEntity(questId: Int): QuestObjectiveEntity =
    QuestObjectiveEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        status = status.toQuestObjectiveStatus(),
        orderIndex = orderIndex,
        isOptional = isOptional
    )

private fun BackupQuestReward.toEntity(questId: Int): QuestRewardEntity =
    QuestRewardEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type.toQuestRewardType()
    )

private fun BackupQuestConsequence.toEntity(questId: Int): QuestConsequenceEntity =
    QuestConsequenceEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type.toQuestConsequenceType()
    )

private fun BackupQuestNote.toEntity(questId: Int): QuestNoteEntity =
    QuestNoteEntity(
        id = id,
        questId = questId,
        content = content,
        createdAt = createdAt,
        // Carried, never re-stamped: a restore is not an edit, and recomputing this would make
        // every note look as if it had just been revised.
        updatedAt = updatedAt
    )

/**
 * An unknown reward or consequence kind degrades to its `OTHER`, which is exactly what that
 * constant is for: the text the DM wrote is worth more than the drawer it was filed in.
 */
private fun String.toQuestRewardType(): QuestRewardType =
    QuestRewardType.entries.firstOrNull { it.name == this } ?: QuestRewardType.OTHER

private fun String.toQuestConsequenceType(): QuestConsequenceType =
    QuestConsequenceType.entries.firstOrNull { it.name == this } ?: QuestConsequenceType.OTHER

/**
 * An unknown objective status degrades to [QuestObjectiveStatus.NOT_STARTED].
 *
 * Unlike a quest's status, here the cautious answer is also the right one: a step this build
 * cannot name has not been accounted for, and claiming it was done would quietly mark work
 * complete that nobody did.
 */
private fun String.toQuestObjectiveStatus(): QuestObjectiveStatus =
    QuestObjectiveStatus.entries.firstOrNull { it.name == this } ?: QuestObjectiveStatus.NOT_STARTED

/**
 * A category name this build does not know degrades to [QuestCategory.OTHER], which is what that
 * constant is for: the quest itself is worth more than the drawer it was filed in.
 */
private fun String.toQuestCategory(): QuestCategory =
    QuestCategory.entries.firstOrNull { it.name == this } ?: QuestCategory.OTHER

/**
 * An unknown status degrades to [QuestStatus.ACTIVE].
 *
 * There is no neutral member to fall back on here, so the choice is which wrong answer is the one
 * the DM will notice: an active quest sits in plain sight on the list and gets corrected, while
 * NOT_STARTED or COMPLETED would file a live quest away behind a filter and never be questioned.
 */
private fun String.toQuestStatus(): QuestStatus =
    QuestStatus.entries.firstOrNull { it.name == this } ?: QuestStatus.ACTIVE

/** An unknown priority degrades to the middle one, which claims the least. */
private fun String.toQuestPriority(): QuestPriority =
    QuestPriority.entries.firstOrNull { it.name == this } ?: QuestPriority.NORMAL

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
