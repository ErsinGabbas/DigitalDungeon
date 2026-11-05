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

    suspend fun updateCampaignParticipants(
        campaignId: Int,
        participants: List<EventParticipant>
    )
}
