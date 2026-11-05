package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.domain.model.SessionEvent
import kotlinx.serialization.json.Json

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
        participants = if (participants.isEmpty()) null else Json.encodeToString(participants)
    )
