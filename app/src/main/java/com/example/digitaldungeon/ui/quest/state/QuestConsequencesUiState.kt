package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the consequences block on the quest screen.
 *
 * Deliberately a separate type from [QuestRewardsUiState] even though the two have the same shape:
 * they hold different domain types and different type enums, and collapsing them behind a generic
 * would make every read site spell out a type argument to save a dozen lines here.
 *
 * @param editingConsequenceId the consequence the form is rewriting, `null` while it is adding.
 * @param formError why the form refused to save. Separate from [error], which is about a write
 *   that failed.
 */
data class QuestConsequencesUiState(
    val consequences: List<QuestConsequence> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val editingConsequenceId: Int? = null,
    val formTitle: String = "",
    val formDescription: String = "",
    val formType: QuestConsequenceType = QuestConsequenceType.OTHER,
    val formError: UiText? = null,
    val pendingDeletion: PendingConsequenceDeletion? = null
) {

    val isEditing: Boolean
        get() = editingConsequenceId != null

    /**
     * Nothing has come of the quest yet.
     *
     * An empty list here means the story has not caught up, not that something is missing - which
     * is why the screen says so in those words.
     */
    val hasNoConsequences: Boolean
        get() = consequences.isEmpty() && !isLoading
}
