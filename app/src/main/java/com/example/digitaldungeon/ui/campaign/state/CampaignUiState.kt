package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.Campaign

data class CampaignUiState(
    val campaigns: List<Campaign> = emptyList(),
    val campaignName: String = "",
    val campaignDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * Non-null while the delete confirmation dialog is open. Lives in the state
     * instead of a `remember` so the question survives a configuration change.
     */
    val pendingDeletion: PendingCampaignDeletion? = null
)
