package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class CampaignRepository(
    private val dao: CampaignDao
) {

    fun getCampaigns() =
        dao.getCampaigns().map { entities ->

            entities.map {
                it.toDomain()
            }

        }

    suspend fun getCampaignById(
        id: Int
    ): Campaign? {

        return dao
            .getCampaignById(id)
            ?.toDomain()
    }

    fun getCampaignByIdFlow(
        id: Int
    ): Flow<Campaign?> =
        dao.observeCampaignById(id).map { entity ->

            entity?.toDomain()

        }

    suspend fun addCampaign(
        name: String,
        description: String,
        participants: List<EventParticipant> = emptyList()
    ) {

        val participantsJson = if (participants.isEmpty()) null else Json.encodeToString(participants)

        dao.insertCampaign(
            CampaignEntity(
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                participants = participantsJson
            )
        )
    }

    suspend fun deleteCampaign(
        id: Int
    ) {

        dao.deleteCampaign(id)

    }

    suspend fun updateCampaignParticipants(
        campaignId: Int,
        participants: List<EventParticipant>
    ) {
        val participantsJson = if (participants.isEmpty()) null else Json.encodeToString(participants)
        dao.updateCampaignParticipants(campaignId, participantsJson)
    }
}
