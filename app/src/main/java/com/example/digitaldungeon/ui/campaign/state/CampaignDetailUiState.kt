package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.Campaign

/**
 * State of the campaign itself on the campaign detail screen.
 *
 * Kept apart from the session and participant states so that a campaign load
 * failure cannot be overwritten by - or overwrite - their errors.
 */
data class CampaignDetailUiState(
    val campaign: Campaign? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
