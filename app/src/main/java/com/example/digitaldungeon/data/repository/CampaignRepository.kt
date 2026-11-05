package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.coroutines.flow.Flow

interface CampaignRepository {

    fun getCampaigns(): Flow<List<Campaign>>

    suspend fun getCampaignById(
        id: Int
    ): Campaign?

    fun getCampaignByIdFlow(
        id: Int
    ): Flow<Campaign?>

    suspend fun addCampaign(
        name: String,
        description: String,
        participants: List<EventParticipant> = emptyList()
    )

    suspend fun deleteCampaign(
        id: Int
    )

    /**
     * Replaces the name and the description of a campaign.
     */
    suspend fun updateCampaign(
        id: Int,
        name: String,
        description: String
    )

    suspend fun updateCampaignParticipants(
        campaignId: Int,
        participants: List<EventParticipant>
    )
}
