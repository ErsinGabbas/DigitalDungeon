package com.example.digitaldungeon.data.backup

import kotlinx.serialization.Serializable

/**
 * Version of the backup *document format*, i.e. the one this app writes.
 *
 * Deliberately unrelated to the Room schema version: the database may be migrated, split or
 * renamed without changing what a backup file looks like, and a backup file may gain a field
 * without touching the database. Only bump this when the shape below changes in a way an older
 * build cannot read, so an import can tell what it was handed and refuse it with a clear message
 * instead of writing garbage.
 *
 * Version 2 added [BackupCampaign.quests], version 3 [BackupQuest.objectives], version 4 the
 * rewards, consequences and notes of a quest, version 5 [BackupCampaign.questRelations], and
 * version 6 the history of a quest together with [BackupCampaign.questSessionLinks]. Every earlier
 * version is still read, see [SUPPORTED_BACKUP_FORMAT_VERSIONS].
 */
const val BACKUP_FORMAT_VERSION = 6

/**
 * Every format version this build can restore.
 *
 * A set rather than a single constant, because bumping the format must not turn the copies people
 * already have into unreadable files: a version 1 document simply carries no quests, a version 2
 * one no objectives, a version 3 one no rewards, consequences or notes, a version 4 one no links
 * between quests, a version 5 one no history and no links to sessions, and those fields defaulting
 * to an empty list is what makes reading them a non-event. What is refused is only a document from
 * the *future* - a file whose fields this build has never heard of cannot be restored without
 * silently dropping whatever is in them.
 *
 * Every version this app has ever written stays in here. Dropping one would turn a backup somebody
 * made years ago into an unreadable file, which is the one thing a backup may never become.
 */
val SUPPORTED_BACKUP_FORMAT_VERSIONS = setOf(1, 2, 3, 4, 5, 6)

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

    val sessions: List<BackupSession> = emptyList(),

    /**
     * The links between the quests of this campaign. Added in format version 5.
     *
     * Nested under the campaign and not under either quest, because a link belongs to neither end:
     * putting it inside one of them would make the document claim a direction the data does not
     * have for the symmetric types, and would have to be read twice for the directional ones.
     * Both ids are therefore written out, and a link whose ends are not both in this campaign
     * cannot be expressed at all - which is the same rule the repository enforces on the way in.
     */
    val questRelations: List<BackupQuestRelation> = emptyList(),

    /**
     * Which quests came up in which sessions. Added in format version 6.
     *
     * Nested under the campaign for the same reason the links between quests are: the row belongs
     * to neither end, and both ids have to be written out. Both ends are in this campaign by the
     * rule the repository enforces, so a link that reached outside it cannot be expressed here.
     */
    val questSessionLinks: List<BackupQuestSessionLink> = emptyList(),

    /**
     * The quests of this campaign. Added in format version 2; defaulting to empty is what lets a
     * version 1 document - written before the quest tracker existed - restore unchanged.
     *
     * A sibling of [sessions] rather than something nested inside one: a quest belongs to the
     * campaign and may well have been handed out before any session was written down.
     */
    val quests: List<BackupQuest> = emptyList()
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

/**
 * One quest of the campaign it is nested under.
 *
 * [category], [status] and [priority] travel as plain text holding the enum constant name, for the
 * same reason [BackupSessionEvent.type] does: a constant a future version adds must not make a
 * whole file unreadable to this one.
 */
@Serializable
data class BackupQuest(

    val id: Int,

    val title: String,

    val description: String,

    /** Name of a `QuestCategory` constant. */
    val category: String,

    /** Name of a `QuestStatus` constant. */
    val status: String,

    /** Name of a `QuestPriority` constant. */
    val priority: String,

    val isHidden: Boolean = false,

    val createdAt: Long,

    /** Free text in the world's own calendar, absent for most quests. */
    val inGameDate: String? = null,

    /**
     * The steps of this quest, in their stored order. Added in format version 3; defaulting to
     * empty is what lets a version 1 or 2 document restore unchanged.
     */
    val objectives: List<BackupQuestObjective> = emptyList(),

    /** What the quest is worth. Added in format version 4. */
    val rewards: List<BackupQuestReward> = emptyList(),

    /** What came of it. Added in format version 4. */
    val consequences: List<BackupQuestConsequence> = emptyList(),

    /** The DM's notes on it. Added in format version 4. */
    val notes: List<BackupQuestNote> = emptyList(),

    /** Its history, newest last as the table stores it. Added in format version 6. */
    val history: List<BackupQuestHistoryEntry> = emptyList()
)

/**
 * One record that a quest came up in a session of the campaign it is nested under.
 */
@Serializable
data class BackupQuestSessionLink(

    val id: Int,

    val questId: Int,

    val sessionId: Int
)

/**
 * One entry of a quest's history log.
 *
 * [description] is data and not a sentence - the name of a status constant, or an objective's
 * title, depending on [eventType] - exactly as it is in the table. A document that stored finished
 * sentences would be a document in one language.
 */
@Serializable
data class BackupQuestHistoryEntry(

    val id: Int,

    /** Name of a `QuestHistoryEventType` constant. */
    val eventType: String,

    val description: String,

    val timestamp: Long
)

/**
 * One link between two quests of the campaign it is nested under.
 *
 * [relationType] travels as plain text holding the enum constant name, like every other enum here.
 * The pair is already normalised when it is exported - the smaller id first for the symmetric
 * types, `FOLLOWS` long since rewritten as `PRECEDES` - so a restore writes the rows back as they
 * stand rather than normalising them a second time.
 */
@Serializable
data class BackupQuestRelation(

    val id: Int,

    val sourceQuestId: Int,

    val targetQuestId: Int,

    /** Name of a `QuestRelationType` constant. */
    val relationType: String
)

/**
 * One reward of the quest it is nested under.
 *
 * [type] travels as plain text holding the enum constant name, for the same reason every other
 * enum in this document does: a constant a future version adds must not make a whole file
 * unreadable to this one.
 */
@Serializable
data class BackupQuestReward(

    val id: Int,

    val title: String,

    val description: String,

    /** Name of a `QuestRewardType` constant. */
    val type: String
)

/** One consequence of the quest it is nested under. */
@Serializable
data class BackupQuestConsequence(

    val id: Int,

    val title: String,

    val description: String,

    /** Name of a `QuestConsequenceType` constant. */
    val type: String
)

/**
 * One note on the quest it is nested under.
 *
 * Both timestamps are carried, because their difference is the only thing that says a note was
 * ever revised; recomputing either on restore would erase that.
 */
@Serializable
data class BackupQuestNote(

    val id: Int,

    val content: String,

    val createdAt: Long,

    val updatedAt: Long
)

/**
 * One objective of the quest it is nested under.
 *
 * [orderIndex] is carried rather than inferred from the list position, for the same reason the
 * session timeline carries it: restoring the order is what keeps the steps in the sequence the DM
 * put them in, and a document that had been hand-edited into a different order should restore as
 * it reads.
 */
@Serializable
data class BackupQuestObjective(

    val id: Int,

    val title: String,

    val description: String,

    /** Name of a `QuestObjectiveStatus` constant. */
    val status: String,

    val orderIndex: Int,

    val isOptional: Boolean = false
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
