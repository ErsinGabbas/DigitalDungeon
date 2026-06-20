package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEventDao {

    @Query(
        "SELECT * FROM session_events WHERE sessionId = :sessionId ORDER BY orderIndex ASC"
    )
    fun getEventsForSession(
        sessionId: Int
    ): Flow<List<SessionEventEntity>>

    @Query(
        "SELECT MAX(orderIndex) FROM session_events WHERE sessionId = :sessionId"
    )
    suspend fun getMaxOrderIndex(sessionId: Int): Int?

    @Insert
    suspend fun insertEvent(
        event: SessionEventEntity
    ): Long

    @Update
    suspend fun updateEvent(
        event: SessionEventEntity
    )

    @Query(
        "DELETE FROM session_events WHERE id = :id"
    )
    suspend fun deleteEvent(
        id: Int
    )

    @Query(
        "DELETE FROM session_events WHERE sessionId = :sessionId"
    )
    suspend fun deleteEventsForSession(
        sessionId: Int
    )
}
