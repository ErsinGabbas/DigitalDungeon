package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.domain.model.Campaign

data class CampaignUiState(
    val campaigns: List<Campaign> = emptyList(),
    val campaignName: String = "",
    val campaignDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)