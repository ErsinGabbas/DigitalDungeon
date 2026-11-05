package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignWithParticipants
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Transaction
    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getCampaigns(): Flow<List<CampaignWithParticipants>>

    @Insert
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Int)

    @Query("UPDATE campaigns SET name = :name, description = :description WHERE id = :id")
    suspend fun updateCampaign(id: Int, name: String, description: String)

    @Transaction
    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(
        id: Int
    ): CampaignWithParticipants?

    @Transaction
    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun observeCampaignById(
        id: Int
    ): Flow<CampaignWithParticipants?>
}
