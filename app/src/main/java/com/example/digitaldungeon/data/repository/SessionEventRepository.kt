package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.Flow

interface SessionEventRepository {

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
}
