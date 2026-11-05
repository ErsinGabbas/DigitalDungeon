package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.Session

/**
 * State of the session block of the campaign detail screen: the list of the
 * campaign's sessions, the "add session" form and the pending delete request.
 *
 * Kept apart from [CampaignDetailUiState] and [CampaignParticipantsUiState] so
 * that a session error cannot overwrite - or be overwritten by - theirs.
 */
data class CampaignSessionsUiState(
    val sessions: List<Session> = emptyList(),
    val sessionTitle: String = "",
    val sessionNotes: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * Non-null while the delete confirmation dialog is open. Lives in the state
     * instead of a `remember` so the question survives a configuration change.
     */
    val pendingDeletion: PendingSessionDeletion? = null,
    /**
     * Non-null while the edit dialog is open - it is the session being edited.
     * In the state for the same reason as [pendingDeletion]: a rotation must not
     * throw away a half typed title.
     */
    val editingSession: Session? = null,
    val editSessionTitle: String = "",
    val editSessionNotes: String = "",
    /**
     * Why the edit dialog refused to save. Separate from [error] because the
     * dialog covers the screen: a message behind it would never be read.
     */
    val editError: String? = null
)
