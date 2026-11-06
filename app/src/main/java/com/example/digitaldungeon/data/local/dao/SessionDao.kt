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

    /**
     * Every session of every campaign, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM sessions ORDER BY campaignId ASC, id ASC"
    )
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert
    suspend fun insertSessions(
        sessions: List<SessionEntity>
    )

    @Query(
        "DELETE FROM sessions"
    )
    suspend fun deleteAllSessions()

    @Query(
        "UPDATE sessions SET title = :title, notes = :notes WHERE id = :id"
    )
    suspend fun updateSession(
        id: Int,
        title: String,
        notes: String
    )

    @Query(
        "DELETE FROM sessions WHERE id = :id"
    )
    suspend fun deleteSession(
        id: Int
    )
}
