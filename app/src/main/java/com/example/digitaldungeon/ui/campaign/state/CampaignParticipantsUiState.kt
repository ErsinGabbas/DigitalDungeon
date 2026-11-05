package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.EventParticipant

/**
 * Single source of truth for the participant block of the campaign detail screen.
 *
 * [participants] is the one editable collection shown in the UI; [savedParticipants]
 * mirrors what is currently persisted for the campaign, so the difference between the
 * two tells whether there is anything to save.
 */
data class CampaignParticipantsUiState(
    val participants: List<EventParticipant> = emptyList(),
    val savedParticipants: List<EventParticipant> = emptyList(),
    val participantName: String = "",
    val participantLevel: String = "",
    val participantMaxHp: String = "",
    val editingIndex: Int? = null,
    val isSaving: Boolean = false,
    val error: String? = null
) {

    val isEditing: Boolean
        get() = editingIndex != null

    val hasUnsavedChanges: Boolean
        get() = participants != savedParticipants
}
