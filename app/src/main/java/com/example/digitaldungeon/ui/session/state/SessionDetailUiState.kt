package com.example.digitaldungeon.ui.session.state

import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

data class SessionDetailUiState(
    val session: Session? = null,
    val events: List<SessionEvent> = emptyList(),
    val quickNoteText: String = "",
    val selectedEventType: SessionEventType = SessionEventType.NOTE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val editingEvent: SessionEvent? = null,
    // Edit dialog fields
    val editTitle: String = "",
    val editType: SessionEventType = SessionEventType.NOTE,
    val editContent: String = "",
    // Delete confirmation dialog: the event the user is about to delete
    val eventPendingDeletion: SessionEvent? = null
) {
    /**
     * Single source of truth for "is the quick note worth adding".
     * The screen uses it to enable the Add button and the ViewModel uses it to
     * guard [com.example.digitaldungeon.ui.session.viewmodel.SessionDetailViewModel.onAddQuickNote],
     * so the rule lives in exactly one place.
     */
    val canAddQuickNote: Boolean
        get() = quickNoteText.isNotBlank()
}
