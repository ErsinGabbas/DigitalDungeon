package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the quest list of one campaign.
 *
 * @param campaignId the campaign the screen was opened with, known before anything has loaded -
 *   it is what every read is scoped by, so a quest of another campaign cannot appear here.
 * @param quests every quest of the campaign, as stored. Searching and filtering never shrink this
 *   list; [visibleQuests] is the view the screen draws.
 */
data class QuestListUiState(
    val campaignId: Int = 0,
    val quests: List<Quest> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    /** What the user typed into the search field, "" while not searching. */
    val searchQuery: String = "",
    /** `null` means "any status", otherwise only quests in that status are shown. */
    val statusFilter: QuestStatus? = null,
    /** `null` means "any category". */
    val categoryFilter: QuestCategory? = null,
    val sort: QuestSort = QuestSort.PRIORITY,
    /**
     * Non-null while the delete confirmation dialog is open. In the state rather than in a
     * `remember`, so the question survives a configuration change.
     */
    val pendingDeletion: PendingQuestDeletion? = null
) {

    /**
     * What the list shows: [quests] passed through the search and both filters, then ordered by
     * [sort].
     *
     * Computed once per state instance instead of on every read, so the screen can hand it
     * straight to a `LazyColumn`. Derived and never stored, for the same reason the campaign list
     * and the session timeline derive theirs: the filtered list must not displace the full one, or
     * "this campaign has no quests yet" becomes indistinguishable from "nothing matched".
     */
    val visibleQuests: List<Quest> = quests
        .filter { quest ->
            (statusFilter == null || quest.status == statusFilter) &&
                (categoryFilter == null || quest.category == categoryFilter) &&
                quest.matches(searchQuery.trim())
        }
        .sortedWith(sort.comparator())

    /** True while the list on screen is a subset of [quests]. */
    val isFilterActive: Boolean
        get() = searchQuery.isNotBlank() || statusFilter != null || categoryFilter != null

    /** Nothing has been written down yet - the "add your first quest" case. */
    val hasNoQuests: Boolean
        get() = quests.isEmpty() && !isLoading

    /**
     * There are quests, the search and the filters just hid all of them. A different state from
     * [hasNoQuests] and worth a different message - only one of the two is the user's own doing.
     */
    val hasNoMatches: Boolean
        get() = quests.isNotEmpty() && visibleQuests.isEmpty()
}

/**
 * Case insensitive match on the title or the description.
 *
 * The description is searched as well as the title, like a campaign's is: "the miller" is as
 * likely to be in the body of a quest as in its name.
 */
private fun Quest.matches(needle: String): Boolean {
    if (needle.isEmpty()) return true

    return title.contains(needle, ignoreCase = true) ||
        description.contains(needle, ignoreCase = true)
}

/**
 * The ordering of a [QuestSort].
 *
 * `sortedWith` on a stable sort, which is what makes "newest first within the same priority" come
 * out for free: the list arrives from the database ordered by creation date, and equal priorities
 * keep that order. The date is compared here anyway rather than left implicit, so the screen does
 * not quietly depend on the `ORDER BY` of a query it never sees.
 */
private fun QuestSort.comparator(): Comparator<Quest> =
    when (this) {
        QuestSort.PRIORITY ->
            compareBy<Quest> { it.priority }
                .thenByDescending { it.createdAt }

        QuestSort.NEWEST ->
            compareByDescending { it.createdAt }
    }
