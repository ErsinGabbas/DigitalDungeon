package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.Campaign
import kotlinx.coroutines.flow.map

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
    ): Campaign {

        return dao
            .getCampaignById(id)
            .toDomain()
    }

    suspend fun addCampaign(
        name: String,
        description: String
    ) {

        dao.insertCampaign(
            CampaignEntity(
                name = name,
                description = description,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCampaign(
        id: Int
    ) {

        dao.deleteCampaign(id)

    }
}