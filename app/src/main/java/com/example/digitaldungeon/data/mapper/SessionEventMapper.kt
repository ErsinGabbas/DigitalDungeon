package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEvent

/**
 * Encodes a participant snapshot into the payload stored in `session_events.participants`.
 *
 * The single place where that column is written, so every writer - the repository, the backup
 * restore - produces the exact same shape that [decodeParticipants] reads back.
 *
 * An empty snapshot is stored as `NULL` instead of `"[]"`, which keeps events without
 * participants indistinguishable from the rows written before the column existed.
 */
internal fun encodeParticipants(participants: List<EventParticipant>): String? =
    if (participants.isEmpty()) {
        null
    } else {
        mapperJson.encodeToString(participants)
    }

fun SessionEventEntity.toDomain(): SessionEvent =
    SessionEvent(
        id = id,
        sessionId = sessionId,
        type = type,
        title = title,
        content = content,
        orderIndex = orderIndex,
        timestamp = timestamp,
        participants = decodeParticipants(participants)
    )

fun SessionEvent.toEntity(): SessionEventEntity =
    SessionEventEntity(
        id = id,
        sessionId = sessionId,
        type = type,
        title = title,
        content = content,
        orderIndex = orderIndex,
        timestamp = timestamp,
        participants = encodeParticipants(participants)
    )
