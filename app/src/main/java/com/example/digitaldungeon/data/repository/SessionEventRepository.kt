package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.Flow

interface SessionEventRepository {

    /** Direction in which an event is shifted on the timeline. */
    enum class MoveDirection {
        UP,
        DOWN
    }

    fun getEventsForSession(sessionId: Int): Flow<List<SessionEvent>>

    suspend fun addEvent(
        sessionId: Int,
        type: SessionEventType,
        title: String?,
        content: String
    )

    suspend fun deleteEvent(id: Int)

    suspend fun updateEvent(
        eventId: Int,
        title: String,
        type: SessionEventType,
        content: String
    )

    /**
     * Swaps the event with the neighbour above or below it on the timeline.
     *
     * Moving the first event [MoveDirection.UP] or the last one [MoveDirection.DOWN]
     * (or passing an unknown [eventId]) is a no-op.
     */
    suspend fun moveEvent(
        eventId: Int,
        direction: MoveDirection
    )
}
