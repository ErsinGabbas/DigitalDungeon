package com.example.digitaldungeon.domain.model

/**
 * One line of a quest's history: something the DM did to it, and when.
 *
 * The log is append-only. There is no edit and no single-row delete, because a history that can be
 * rewritten is not a history - it is just another editable list, and the one thing this screen is
 * for is being able to trust it. Rows leave only when the quest does, by cascade.
 *
 * What gets recorded is what the DM did, not everything that happened: status moves on the quest
 * and status moves on its steps. Adding a reward is not history, it is the reward - it sits in its
 * own list and is still there to be looked at, so writing a second copy of it here would only make
 * the log longer without making it say more.
 *
 * Room stores an enum as TEXT by constant name, so renaming a [QuestHistoryEventType] constant
 * orphans every row already written with the old spelling; the names are pinned by a keep rule in
 * `proguard-rules.pro` so a release build cannot rename them either.
 *
 * @param description data, deliberately, and never a finished sentence - what the data *is* depends
 *   on [eventType]: for [QuestHistoryEventType.QUEST_STATUS_CHANGED] it is the name of the new
 *   [QuestStatus] constant, and for the three objective events it is the objective's title. A
 *   formatted sentence would be frozen in whatever language was current when it was written, and
 *   this app resolves every user-facing string at render time instead - see `UiText`.
 * @param timestamp epoch millis, like every other time in this app.
 */
data class QuestHistoryEntry(
    val id: Int,
    val questId: Int,
    val eventType: QuestHistoryEventType,
    val description: String,
    val timestamp: Long
)

/**
 * Which kind of move a history line records, and therefore how its
 * [QuestHistoryEntry.description] is to be read.
 *
 * One constant for the quest's own status and three for its steps, because a step's three states
 * are reached by three different gestures the DM would want to tell apart afterwards - a step
 * reopened by hand is worth seeing as its own line, not as a second
 * [OBJECTIVE_COMPLETED] with the truth hidden in the ordering.
 *
 * Stored as TEXT by constant name, like every other enum in this database.
 */
enum class QuestHistoryEventType {
    QUEST_STATUS_CHANGED,
    OBJECTIVE_COMPLETED,
    OBJECTIVE_FAILED,
    OBJECTIVE_REOPENED
}
