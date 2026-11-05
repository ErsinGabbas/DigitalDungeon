package com.example.digitaldungeon.data.repository

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.CampaignParticipantDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.mapper.toCampaignParticipantEntities
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * A campaign and its roster live in two tables, so every write that touches both goes through a
 * single transaction: readers must never see a campaign with a half-written roster.
 */
class CampaignRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: CampaignDao,
    private val participantDao: CampaignParticipantDao
) : CampaignRepository {

    override fun getCampaigns(): Flow<List<Campaign>> =
        dao.getCampaigns().map { campaigns ->

            campaigns.map {
                it.toDomain()
            }

        }

    override suspend fun getCampaignById(
        id: Int
    ): Campaign? {

        return dao
            .getCampaignById(id)
            ?.toDomain()
    }

    override fun getCampaignByIdFlow(
        id: Int
    ): Flow<Campaign?> =
        dao.observeCampaignById(id).map { campaign ->

            campaign?.toDomain()

        }

    override suspend fun addCampaign(
        name: String,
        description: String,
        participants: List<EventParticipant>
    ) {

        database.withTransaction {

            val campaignId = dao.insertCampaign(
                CampaignEntity(
                    name = name,
                    description = description,
                    createdAt = System.currentTimeMillis()
                )
            ).toInt()

            participantDao.insertParticipants(
                participants.toCampaignParticipantEntities(campaignId)
            )
        }
    }

    override suspend fun deleteCampaign(
        id: Int
    ) {

        dao.deleteCampaign(id)

    }

    override suspend fun updateCampaign(
        id: Int,
        name: String,
        description: String
    ) {

        dao.updateCampaign(id, name, description)

    }

    override suspend fun updateCampaignParticipants(
        campaignId: Int,
        participants: List<EventParticipant>
    ) {
        participantDao.replaceParticipantsForCampaign(
            campaignId,
            participants.toCampaignParticipantEntities(campaignId)
        )
    }
}
