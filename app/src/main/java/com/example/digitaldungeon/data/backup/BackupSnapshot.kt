package com.example.digitaldungeon.data.backup

import kotlinx.serialization.Serializable

/**
 * Version of the backup *document format*.
 *
 * Deliberately unrelated to the Room schema version: the database may be migrated, split or
 * renamed without changing what a backup file looks like, and a backup file may gain a field
 * without touching the database. Only bump this when the shape below changes in a way an older
 * build cannot read, so an import can tell what it was handed and refuse it with a clear message
 * instead of writing garbage.
 */
const val BACKUP_FORMAT_VERSION = 1

/**
 * The whole database as one readable document.
 *
 * These DTOs are intentionally separate from the domain models rather than `@Serializable`
 * annotations on them: a backup written today has to stay readable after the domain has been
 * refactored, so the two contracts are kept apart and only mapped onto each other inside
 * [BackupServiceImpl].
 *
 * The structure is nested - sessions inside their campaign, events inside their session - which
 * encodes the foreign keys positionally. There is no way to write a snapshot whose session points
 * at a campaign that is not in the file.
 */
@Serializable
data class BackupSnapshot(

    /** Always written; an import reads this first and nothing else until it matches. */
    val formatVersion: Int = BACKUP_FORMAT_VERSION,

    /** When the backup was taken, epoch millis. Informational only. */
    val createdAt: Long,

    val campaigns: List<BackupCampaign> = emptyList()
)

@Serializable
data class BackupCampaign(

    /** Primary key, preserved verbatim on restore so the file stays internally linked. */
    val id: Int,

    val name: String,

    val description: String,

    val createdAt: Long,

    /** The queryable roster, i.e. the rows of `campaign_participants`. */
    val participants: List<BackupCampaignParticipant> = emptyList(),

    val sessions: List<BackupSession> = emptyList()
)

@Serializable
data class BackupCampaignParticipant(

    val id: Int,

    val name: String,

    val level: Int,

    val maxHp: Int,

    /** Roster position, kept because a relation query gives no ordering guarantee of its own. */
    val orderIndex: Int
)

@Serializable
data class BackupSession(

    val id: Int,

    val title: String,

    val notes: String,

    val createdAt: Long,

    val events: List<BackupSessionEvent> = emptyList()
)

@Serializable
data class BackupSessionEvent(

    val id: Int,

    /**
     * Name of a `SessionEventType` constant, stored as plain text rather than as the enum so an
     * unknown constant cannot make a whole file unreadable.
     */
    val type: String,

    val title: String? = null,

    val content: String,

    /** Position on the timeline; restoring it is what keeps the log in the user's order. */
    val orderIndex: Int,

    val timestamp: Long,

    /**
     * The immutable snapshot of the party at the moment of the event, copied into the event and
     * never resolved against the campaign roster.
     */
    val participants: List<BackupEventParticipant> = emptyList()
)

@Serializable
data class BackupEventParticipant(

    val name: String,

    val level: Int,

    val maxHp: Int
)
