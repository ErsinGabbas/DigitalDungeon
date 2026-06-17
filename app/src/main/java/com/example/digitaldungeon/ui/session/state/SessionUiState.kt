package com.example.digitaldungeon.ui.session.state

import com.example.digitaldungeon.domain.model.Session
data class SessionUiState(
    val sessions: List<Session> = emptyList(),
    val sessionTitle: String = "",
    val sessionNotes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)