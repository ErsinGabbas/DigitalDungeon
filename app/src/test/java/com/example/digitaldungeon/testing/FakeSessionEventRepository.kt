package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.SessionEventRepository
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [SessionEventRepository]; see [FakeCampaignRepository] for
 * the conventions (state-flow driven emissions, recorded calls, injectable failures).
 */
class FakeSessionEventRepository : SessionEventRepository {

    val events = MutableStateFlow<List<SessionEvent>>(emptyList())

    var getEventsForSessionError: Exception? = null
    var addEventError: Exception? = null
    var deleteEventError: Exception? = null
    var updateEventError: Exception? = null

    data class AddEventCall(
        val sessionId: Int,
        val type: SessionEventType,
        val title: String?,
        val content: String
    )

    data class UpdateEventCall(
        val eventId: Int,
        val title: String,
        val type: SessionEventType,
        val content: String
    )

    val addEventCalls = mutableListOf<AddEventCall>()
    val updateEventCalls = mutableListOf<UpdateEventCall>()
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
        content: String
    ) {
        addEventCalls += AddEventCall(sessionId, type, title, content)
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
        content: String
    ) {
        updateEventCalls += UpdateEventCall(eventId, title, type, content)
        updateEventError?.let { throw it }
    }
}
