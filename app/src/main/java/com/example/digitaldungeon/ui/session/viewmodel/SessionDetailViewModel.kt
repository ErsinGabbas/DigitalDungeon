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
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val sessions = sessionRepository.getSessionsForCampaign(0)
                sessions.collect { sessionList ->
                    val session = sessionList.find { it.id == sessionId }
                    _uiState.value = _uiState.value.copy(
                        session = session,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
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
                        _uiState.value = _uiState.value.copy(events = events)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------
    // QUICK NOTE
    // -------------------------
    fun onQuickNoteTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(quickNoteText = text)
    }

    fun onEventTypeSelected(type: SessionEventType) {
        _uiState.value = _uiState.value.copy(selectedEventType = type)
    }

    fun onAddQuickNote() {
        viewModelScope.launch {
            try {
                val text = _uiState.value.quickNoteText
                if (text.isBlank()) return@launch

                sessionEventRepository.addEvent(
                    sessionId = sessionId,
                    type = _uiState.value.selectedEventType,
                    title = null,
                    content = text
                )

                _uiState.value = _uiState.value.copy(quickNoteText = "")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    fun onDeleteEvent(eventId: Int) {
        viewModelScope.launch {
            try {
                sessionEventRepository.deleteEvent(eventId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------
    // EDIT (НОВОЕ)
    // -------------------------

    fun onEditEvent(event: SessionEvent) {
        _uiState.value = _uiState.value.copy(
            editingEvent = event,
            quickNoteText = event.content
        )
    }

    fun onEditDialogDismiss() {
        _uiState.value = _uiState.value.copy(
            editingEvent = null,
            quickNoteText = ""
        )
    }

    fun onUpdateEvent(eventId: Int, newContent: String) {
        viewModelScope.launch {
            try {
                sessionEventRepository.updateEventContent(
                    eventId = eventId,
                    content = newContent
                )

                _uiState.value = _uiState.value.copy(
                    editingEvent = null,
                    quickNoteText = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------
    // ERROR
    // -------------------------
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
