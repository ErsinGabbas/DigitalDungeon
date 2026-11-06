package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.SessionEventRepository
import com.example.digitaldungeon.data.repository.SessionEventRepository.MoveDirection
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [SessionEventRepository]; see [FakeCampaignRepository] for
 * the conventions (state-flow driven emissions, recorded calls, injectable failures).
 *
 * The recorded calls carry `participants` as well, because the whole point of that
 * argument is that it is written through unchanged: an event's cast is a snapshot the
 * repository stores verbatim, so "which list arrived here" is the only place a test can
 * see whether a text-only edit preserved it.
 */
class FakeSessionEventRepository : SessionEventRepository {

    val events = MutableStateFlow<List<SessionEvent>>(emptyList())

    var getEventsForSessionError: Exception? = null
    var addEventError: Exception? = null
    var deleteEventError: Exception? = null
    var updateEventError: Exception? = null
    var moveEventError: Exception? = null

    data class AddEventCall(
        val sessionId: Int,
        val type: SessionEventType,
        val title: String?,
        val content: String,
        val participants: List<EventParticipant> = emptyList()
    )

    data class UpdateEventCall(
        val eventId: Int,
        val title: String,
        val type: SessionEventType,
        val content: String,
        val participants: List<EventParticipant> = emptyList()
    )

    data class MoveEventCall(
        val eventId: Int,
        val direction: MoveDirection
    )

    val addEventCalls = mutableListOf<AddEventCall>()
    val updateEventCalls = mutableListOf<UpdateEventCall>()
    val moveEventCalls = mutableListOf<MoveEventCall>()
    val deleteEventCalls = mutableListOf<Int>()
    val getEventsForSessionCalls = mutableListOf<Int>()

    override fun getEventsForSession(sessionId: Int): Flow<List<SessionEvent>> {
        getEventsForSessionCalls += sessionId
        return getEventsForSessionError?.let { error -> flow<List<SessionEvent>> { throw error } }
            ?: events
    }

    override suspend fun addEvent(
        sessionId: Int,
        type: SessionEventType,
        title: String?,
        content: String,
        participants: List<EventParticipant>
    ) {
        addEventCalls += AddEventCall(sessionId, type, title, content, participants)
        addEventError?.let { throw it }
    }

    override suspend fun deleteEvent(id: Int) {
        deleteEventCalls += id
        deleteEventError?.let { throw it }
    }

    override suspend fun updateEvent(
        eventId: Int,
        title: String,
        type: SessionEventType,
        content: String,
        participants: List<EventParticipant>
    ) {
        updateEventCalls += UpdateEventCall(eventId, title, type, content, participants)
        updateEventError?.let { throw it }
    }

    override suspend fun moveEvent(
        eventId: Int,
        direction: MoveDirection
    ) {
        moveEventCalls += MoveEventCall(eventId, direction)
        moveEventError?.let { throw it }
    }
}
