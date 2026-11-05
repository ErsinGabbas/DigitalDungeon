package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignParticipantDao {

    @Query(
        "SELECT * FROM campaign_participants WHERE campaignId = :campaignId ORDER BY orderIndex ASC"
    )
    fun getParticipantsForCampaign(
        campaignId: Int
    ): Flow<List<CampaignParticipantEntity>>

    @Insert
    suspend fun insertParticipants(
        participants: List<CampaignParticipantEntity>
    )

    @Query(
        "DELETE FROM campaign_participants WHERE campaignId = :campaignId"
    )
    suspend fun deleteParticipantsForCampaign(
        campaignId: Int
    )

    /**
     * Swaps the whole roster of a campaign in one transaction, so a reader never observes a
     * half-written roster.
     */
    @Transaction
    suspend fun replaceParticipantsForCampaign(
        campaignId: Int,
        participants: List<CampaignParticipantEntity>
    ) {
        deleteParticipantsForCampaign(campaignId)
        insertParticipants(participants)
    }
}
