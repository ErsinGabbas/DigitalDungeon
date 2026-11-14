package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of one quest.
 *
 * @param questId the id the screen was opened with, available before the quest has loaded.
 * @param quest `null` both while loading and when there is no such quest - the two are told apart
 *   by [isLoading], which is what [isMissing] does.
 */
data class QuestDetailUiState(
    val questId: Int = 0,
    val quest: Quest? = null,
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null
) {

    /**
     * There is no quest with this id: it was deleted, here or elsewhere.
     *
     * Not an error, which is why it does not travel in [error]: a quest that is gone is a fact
     * about the journal, not a failure of the screen.
     */
    val isMissing: Boolean
        get() = quest == null && !isLoading
}
