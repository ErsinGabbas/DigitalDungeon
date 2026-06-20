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
    val error: String? = null
)
