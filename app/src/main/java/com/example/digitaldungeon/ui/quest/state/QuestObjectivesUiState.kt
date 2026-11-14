package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the objectives block on the quest screen.
 *
 * Kept apart from [QuestDetailUiState] so that a failure to load the steps cannot overwrite - or
 * be overwritten by - the quest's own error, the same way the campaign screen keeps its three
 * states separate. It is also what stops a keystroke in the objective form from recomposing the
 * quest above it.
 *
 * @param objectives the steps as stored, already in their order.
 * @param editingObjectiveId the objective the form is currently rewriting, `null` while the form
 *   is adding a new one. One form serves both, like the participant editor on the campaign screen.
 * @param formTitle what the form's first field holds. In the state and not in a `remember`, so a
 *   half typed step survives a rotation.
 * @param formError why the form refused to save - the step had no title. Separate from [error],
 *   which is about a write that failed: one is the user's to fix, the other is not.
 */
data class QuestObjectivesUiState(
    val objectives: List<QuestObjective> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val editingObjectiveId: Int? = null,
    val formTitle: String = "",
    val formDescription: String = "",
    val formIsOptional: Boolean = false,
    val formError: UiText? = null,
    /** Non-null while the delete confirmation is on screen. */
    val pendingDeletion: PendingObjectiveDeletion? = null
) {

    /** True while the form is rewriting an existing objective rather than adding one. */
    val isEditing: Boolean
        get() = editingObjectiveId != null

    /**
     * The quest has no steps - which is a normal, complete state for a quest and therefore worth
     * saying out loud rather than leaving as blank space.
     */
    val hasNoObjectives: Boolean
        get() = objectives.isEmpty() && !isLoading

    /**
     * Whether the objective at [index] can move up.
     *
     * Position is the only thing that can block a move here: unlike the session timeline, this
     * list is never filtered, so there is no second reason to explain and no need for the verdict
     * type `EventMoveAvailability` carries.
     */
    fun canMoveUp(index: Int): Boolean = index > 0

    /** Whether the objective at [index] can move down. */
    fun canMoveDown(index: Int): Boolean = index < objectives.lastIndex
}
