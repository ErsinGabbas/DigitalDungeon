package com.example.digitaldungeon.domain.model

/**
 * One assignment the party can take on, inside one campaign.
 *
 * A quest stands on its own: it belongs to a campaign and to nothing else. It is not tied to the
 * session it was handed out in, because a quest outlives the evening it appeared in and most of
 * them are never finished in the session that started them.
 *
 * Only [title] is asked of the user; everything else has a default the form starts out with, which
 * is what makes writing down "the miller wants his daughter back" a single step.
 *
 * @param isHidden the party does not know about this one yet. A marker and a filter, nothing more:
 *   this app has no player-facing side, so everything in it is already private to the DM, and
 *   pretending otherwise would promise an access control that does not exist.
 * @param inGameDate when this happened in the world, as the DM writes it - "14th of Neth, 1492 DR",
 *   "third winter". A string and not a date: fantasy calendars are arbitrary, and a Long would
 *   claim a precision no campaign has.
 */
data class Quest(
    val id: Int,
    val campaignId: Int,
    val title: String,
    val description: String,
    val category: QuestCategory,
    val status: QuestStatus,
    val priority: QuestPriority,
    val isHidden: Boolean,
    val createdAt: Long,
    val inGameDate: String? = null
)

/**
 * What kind of assignment this is.
 *
 * Room stores these as TEXT by constant name, so a rename breaks already stored rows - see the
 * keep rule in `proguard-rules.pro`. The declaration order is the order the UI offers them in.
 *
 * Extensible by adding a constant and a label to `questCategoryLabelRes`, whose `when` is
 * exhaustive and therefore refuses to compile until the label exists. A user-defined category
 * system is deliberately not part of this.
 */
enum class QuestCategory {
    MAIN,
    SIDE,
    PERSONAL,
    FACTION,
    REPEATABLE,
    OTHER
}

/**
 * Where the quest stands.
 *
 * Changed by hand, always. Completing every objective does not complete the quest: whether the
 * miller's daughter being found is the end of it is a judgement about the story, and only the DM
 * makes it. For the same reason nothing here is terminal - [COMPLETED], [FAILED] and [CANCELLED]
 * can all be left again, because a quest everyone thought was over is a normal way for a campaign
 * to continue.
 *
 * Stored as TEXT by name, like [QuestCategory].
 */
enum class QuestStatus {
    NOT_STARTED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * How much this one matters right now.
 *
 * Declared from most to least important on purpose: Kotlin enums compare by ordinal, so sorting a
 * quest list by priority is `sortedBy { it.priority }` and needs no weight table beside it. What is
 * persisted is the constant *name*, so reordering this list changes the sort order without
 * touching a single stored row - the two are independent by construction.
 */
enum class QuestPriority {
    HIGH,
    NORMAL,
    LOW
}
