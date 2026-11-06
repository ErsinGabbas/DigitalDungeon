package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the campaign itself on the campaign detail screen.
 *
 * Kept apart from the session and participant states so that a campaign load
 * failure cannot be overwritten by - or overwrite - their errors.
 */
data class CampaignDetailUiState(
    /** Id the screen was opened with, available before the campaign has loaded. */
    val campaignId: Int = 0,
    val campaign: Campaign? = null,
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null
)
