package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the notes block on the quest screen.
 *
 * Simpler than the reward and consequence blocks because a note is simpler: one field of free text,
 * no title and no type, so the form is one text area and the state has one value to carry.
 *
 * @param notes newest first, which is the order they arrive in from the database.
 * @param editingNoteId the note the form is rewriting, `null` while it is adding a new one.
 * @param formError why the form refused to save - the note was empty.
 */
data class QuestNotesUiState(
    val notes: List<QuestNote> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val editingNoteId: Int? = null,
    val formContent: String = "",
    val formError: UiText? = null,
    val pendingDeletion: PendingNoteDeletion? = null
) {

    val isEditing: Boolean
        get() = editingNoteId != null

    val hasNoNotes: Boolean
        get() = notes.isEmpty() && !isLoading
}
