package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.domain.model.SessionEventType
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

    @Query("UPDATE session_events SET title = :title, type = :type, content = :content WHERE id = :eventId")
    suspend fun updateEvent(eventId: Int, title: String, type: SessionEventType, content: String)

    @Query(
        "SELECT sessionId FROM session_events WHERE id = :eventId"
    )
    suspend fun getSessionIdForEvent(eventId: Int): Int?

    @Query(
        "SELECT * FROM session_events WHERE sessionId = :sessionId ORDER BY orderIndex ASC, id ASC"
    )
    suspend fun getEventsForSessionSnapshot(sessionId: Int): List<SessionEventEntity>

    @Query(
        "UPDATE session_events SET orderIndex = :orderIndex WHERE id = :eventId"
    )
    suspend fun updateEventOrderIndex(eventId: Int, orderIndex: Int)

    /**
     * Swaps an event with its neighbour on the timeline.
     *
     * Works off the actual row order instead of arithmetic on [SessionEventEntity.orderIndex],
     * so gaps left behind by deleted events do not matter. The whole session is renumbered to
     * a dense 0..n-1 sequence inside a single transaction, which also heals pre-existing gaps.
     * Degenerate cases (unknown event, first event moving up, last event moving down) leave the
     * timeline untouched.
     */
    @Transaction
    suspend fun moveEvent(eventId: Int, moveUp: Boolean) {

        val sessionId = getSessionIdForEvent(eventId) ?: return

        val events = getEventsForSessionSnapshot(sessionId)

        val fromPosition = events.indexOfFirst { it.id == eventId }
        if (fromPosition == -1) return

        val toPosition = if (moveUp) fromPosition - 1 else fromPosition + 1
        if (toPosition !in events.indices) return

        val reordered = events.toMutableList()
        reordered[fromPosition] = events[toPosition]
        reordered[toPosition] = events[fromPosition]

        reordered.forEachIndexed { position, event ->

            if (event.orderIndex != position) {
                updateEventOrderIndex(event.id, position)
            }

        }
    }
}
