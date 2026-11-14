package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the form that writes a quest - the same form for a new one and for an existing one.
 *
 * One state class and not two, because the two differ in exactly three things: the heading, where
 * the initial values come from, and which repository call the save ends in. Everything the user
 * types, every validation and the whole layout are identical, and splitting them would mean
 * maintaining that twice.
 *
 * A screen rather than a dialog, unlike the campaign and session editors: seven fields do not fit
 * into an `AlertDialog` without the keyboard covering half of them.
 *
 * @param isEditing false while writing a new quest, true while changing one that exists.
 * @param inGameDate the field's text. A `String` and not a `String?`, because an empty text field
 *   is "" - the blank is turned into `null` on save, which is where the distinction matters.
 * @param areOptionalFieldsVisible whether the second half of the form is unfolded. Starts out
 *   folded for a new quest, which is the promise this module makes - a quest is a title and a line
 *   of description - and unfolded while editing, where folding away fields the quest already has
 *   values in would hide exactly what the user came to change.
 * @param isMissing the quest this form was opened on no longer exists - it was deleted between the
 *   list and here. Nothing to edit and nothing to write: an `UPDATE` would match no row, and the
 *   form would report a save that never happened, so saving is refused instead.
 * @param titleError why the save was refused, shown at the title field. Separate from [error],
 *   which is about the write itself failing: one is the user's to fix, the other is not.
 * @param isSaved the write went through and the screen is done. Read once by the screen to
 *   navigate back; it lives in the state rather than in an event channel so that a rotation
 *   mid-save cannot lose it and leave the user on a form that already saved.
 */
data class QuestEditUiState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val category: QuestCategory = QuestCategory.SIDE,
    val status: QuestStatus = QuestStatus.NOT_STARTED,
    val priority: QuestPriority = QuestPriority.NORMAL,
    val isHidden: Boolean = false,
    val inGameDate: String = "",
    val areOptionalFieldsVisible: Boolean = false,
    /** True while an existing quest is being read into the form. Never true for a new one. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isMissing: Boolean = false,
    val titleError: UiText? = null,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val isSaved: Boolean = false
) {

    /**
     * Whether the save button accepts a press at all.
     *
     * This is about the form being in a state where a write is possible - not about the form being
     * filled in correctly. An empty title deliberately does *not* disable the button: pressing it
     * produces the reason, and a button that silently does nothing explains nothing. A quest that
     * is gone does disable it, because there the reason is already on screen and there is nothing
     * a press could achieve.
     */
    val isSaveEnabled: Boolean
        get() = !isSaving && !isLoading && !isMissing
}
