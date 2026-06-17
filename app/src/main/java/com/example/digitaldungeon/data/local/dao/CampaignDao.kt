package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getCampaigns(): Flow<List<CampaignEntity>>

    @Insert
    suspend fun insertCampaign(campaign: CampaignEntity)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Int)

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(
        id: Int
    ): CampaignEntity
}
