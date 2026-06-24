package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionEventRepository(
    private val dao: SessionEventDao
) {

    fun getEventsForSession(sessionId: Int): Flow<List<com.example.digitaldungeon.domain.model.SessionEvent>> {
        return dao.getEventsForSession(sessionId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    suspend fun addEvent(
        sessionId: Int,
        type: com.example.digitaldungeon.domain.model.SessionEventType,
        title: String?,
        content: String
    ) {
        val maxOrderIndex = dao.getMaxOrderIndex(sessionId) ?: -1
        val newOrderIndex = maxOrderIndex + 1

        dao.insertEvent(
            SessionEventEntity(
                sessionId = sessionId,
                type = type.toEntity(),
                title = title,
                content = content,
                orderIndex = newOrderIndex,
                timestamp = System.currentTimeMillis()
            )
        )
    }


    suspend fun deleteEvent(id: Int) {
        dao.deleteEvent(id)
    }


    suspend fun updateEventContent(eventId: Int, content: String) {
        dao.updateEventContent(eventId, content)
    }
}
