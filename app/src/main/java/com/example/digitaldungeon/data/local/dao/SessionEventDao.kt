package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.local.entity.SessionEventType
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

    @Query("UPDATE session_events SET title = :title, type = :type, content = :content WHERE id = :eventId")
    suspend fun updateEvent(eventId: Int, title: String, type: SessionEventType, content: String)
}
