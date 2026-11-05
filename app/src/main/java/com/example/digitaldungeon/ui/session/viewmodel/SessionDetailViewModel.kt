package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.data.repository.SessionEventRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.state.SessionDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionEventRepository: SessionEventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Int = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        loadEvents()
    }

    // -------------------------
    // SESSION
    // -------------------------
    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            sessionRepository.getSessionById(sessionId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.message,
                            isLoading = false
                        )
                    }
                }
                .collect { session ->
                    // session == null is a valid state (session deleted),
                    // so loading must stop on the very first emission
                    _uiState.update {
                        it.copy(
                            session = session,
                            isLoading = false
                        )
                    }
                }
        }
    }

    // -------------------------
    // EVENTS
    // -------------------------
    private fun loadEvents() {
        viewModelScope.launch {
            try {
                sessionEventRepository.getEventsForSession(sessionId)
                    .collect { events ->
                        _uiState.update { it.copy(events = events) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // -------------------------
    // QUICK NOTE
    // -------------------------
    fun onQuickNoteTextChanged(text: String) {
        _uiState.update { it.copy(quickNoteText = text) }
    }

    fun onEventTypeSelected(type: SessionEventType) {
        _uiState.update { it.copy(selectedEventType = type) }
    }

    fun onAddQuickNote() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                // Same rule the Add button is enabled by, so the two can never disagree
                if (!state.canAddQuickNote) return@launch

                sessionEventRepository.addEvent(
                    sessionId = sessionId,
                    type = state.selectedEventType,
                    title = null,
                    content = state.quickNoteText
                )

                _uiState.update { it.copy(quickNoteText = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    /**
     * Arms the delete confirmation dialog. Nothing is removed until
     * [onConfirmDeleteEvent] is called.
     */
    fun onDeleteEventClick(event: SessionEvent) {
        // Only one dialog at a time: asking to delete closes the edit dialog
        _uiState.update {
            it.clearEditDialog().copy(eventPendingDeletion = event)
        }
    }

    fun onConfirmDeleteEvent() {
        val event = _uiState.value.eventPendingDeletion ?: return

        _uiState.update { it.copy(eventPendingDeletion = null) }

        viewModelScope.launch {
            try {
                sessionEventRepository.deleteEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onDeleteDialogDismiss() {
        _uiState.update { it.copy(eventPendingDeletion = null) }
    }

    // -------------------------
    // EDIT
    // -------------------------
    fun onEditEvent(event: SessionEvent) {
        _uiState.update {
            it.copy(
                // Only one dialog at a time: editing closes a pending delete
                eventPendingDeletion = null,
                editingEvent = event,
                editTitle = event.title ?: "",
                editType = event.type,
                editContent = event.content
            )
        }
    }

    fun onEditTitleChange(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun onEditTypeChange(type: SessionEventType) {
        _uiState.update { it.copy(editType = type) }
    }

    fun onEditContentChange(content: String) {
        _uiState.update { it.copy(editContent = content) }
    }

    fun onUpdateEvent(eventId: Int) {
        viewModelScope.launch {
            try {
                sessionEventRepository.updateEvent(
                    eventId = eventId,
                    title = _uiState.value.editTitle,
                    type = _uiState.value.editType,
                    content = _uiState.value.editContent
                )

                _uiState.update { it.clearEditDialog() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onEditDialogDismiss() {
        _uiState.update { it.clearEditDialog() }
    }

    // -------------------------
    // ERROR
    // -------------------------
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun SessionDetailUiState.clearEditDialog(): SessionDetailUiState =
        copy(
            editingEvent = null,
            editTitle = "",
            editType = SessionEventType.NOTE,
            editContent = ""
        )
}
