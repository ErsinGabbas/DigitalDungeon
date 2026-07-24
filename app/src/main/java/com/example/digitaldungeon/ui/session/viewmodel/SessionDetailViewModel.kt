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
    // EDIT
    // -------------------------
    fun onEditEvent(event: SessionEvent) {
        _uiState.value = _uiState.value.copy(
            editingEvent = event,
            editTitle = event.title ?: "",
            editType = event.type,
            editContent = event.content
        )
    }

    fun onEditTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(editTitle = title)
    }

    fun onEditTypeChange(type: SessionEventType) {
        _uiState.value = _uiState.value.copy(editType = type)
    }

    fun onEditContentChange(content: String) {
        _uiState.value = _uiState.value.copy(editContent = content)
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

                _uiState.value = _uiState.value.copy(
                    editingEvent = null,
                    editTitle = "",
                    editType = SessionEventType.NOTE,
                    editContent = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun onEditDialogDismiss() {
        _uiState.value = _uiState.value.copy(
            editingEvent = null,
            editTitle = "",
            editType = SessionEventType.NOTE,
            editContent = ""
        )
    }

    // -------------------------
    // ERROR
    // -------------------------
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
