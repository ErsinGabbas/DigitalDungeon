package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionEventRepositoryImpl @Inject constructor(
    private val dao: SessionEventDao
) : SessionEventRepository {

    override fun getEventsForSession(sessionId: Int): Flow<List<SessionEvent>> {
        return dao.getEventsForSession(sessionId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun addEvent(
        sessionId: Int,
        type: SessionEventType,
        title: String?,
        content: String
    ) {
        val maxOrderIndex = dao.getMaxOrderIndex(sessionId) ?: -1
        val newOrderIndex = maxOrderIndex + 1

        dao.insertEvent(
            SessionEventEntity(
                sessionId = sessionId,
                type = type,
                title = title,
                content = content,
                orderIndex = newOrderIndex,
                timestamp = System.currentTimeMillis()
            )
        )
    }


    override suspend fun deleteEvent(id: Int) {
        dao.deleteEvent(id)
    }


    override suspend fun updateEvent(
        eventId: Int,
        title: String,
        type: SessionEventType,
        content: String
    ) {
        dao.updateEvent(eventId, title, type, content)
    }


    override suspend fun moveEvent(
        eventId: Int,
        direction: SessionEventRepository.MoveDirection
    ) {
        dao.moveEvent(
            eventId = eventId,
            moveUp = direction == SessionEventRepository.MoveDirection.UP
        )
    }
}
