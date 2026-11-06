package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.EventParticipant
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

    /**
     * Appends an event to the end of the timeline of [sessionId].
     *
     * [participants] is a snapshot of the party as it was when the event happened, copied into
     * the event itself. It is not a reference to the campaign roster: renaming or removing a
     * participant from the campaign later must leave past events untouched.
     */
    suspend fun addEvent(
        sessionId: Int,
        type: SessionEventType,
        title: String?,
        content: String,
        participants: List<EventParticipant> = emptyList()
    )

    suspend fun deleteEvent(id: Int)

    /**
     * Rewrites the editable fields of an event.
     *
     * [participants] replaces the stored snapshot as a whole - passing an empty list clears it -
     * so a caller that only means to edit the text has to hand back the snapshot it already has.
     * Editing an event never consults the campaign roster: the snapshot stays whatever the user
     * last stated it to be.
     */
    suspend fun updateEvent(
        eventId: Int,
        title: String,
        type: SessionEventType,
        content: String,
        participants: List<EventParticipant> = emptyList()
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
