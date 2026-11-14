package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the history block on the quest screen.
 *
 * The only block with no form and no pending deletion, because the log is append-only and nothing
 * on screen writes to it: the entries are appended by the repositories that perform a change, in
 * the same transaction as the change. There is therefore nothing here but what was read.
 */
data class QuestHistoryUiState(
    val entries: List<QuestHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null
) {

    /**
     * Nothing has been done to the quest yet.
     *
     * True for a quest that was only just written down - the log starts empty, because writing a
     * quest down is not something that happened *to* it.
     */
    val hasNoEntries: Boolean
        get() = entries.isEmpty() && !isLoading
}
