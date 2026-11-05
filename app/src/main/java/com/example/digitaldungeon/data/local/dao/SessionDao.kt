package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query(
        "SELECT * FROM sessions WHERE campaignId = :campaignId ORDER BY createdAt DESC"
    )
    fun getSessionsForCampaign(
        campaignId: Int
    ): Flow<List<SessionEntity>>

    @Query(
        "SELECT * FROM sessions WHERE id = :id"
    )
    fun getSessionById(
        id: Int
    ): Flow<SessionEntity?>

    @Insert
    suspend fun insertSession(
        session: SessionEntity
    )

    @Query(
        "DELETE FROM sessions WHERE id = :id"
    )
    suspend fun deleteSession(
        id: Int
    )
}
