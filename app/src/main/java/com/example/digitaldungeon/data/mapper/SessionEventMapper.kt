package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.data.local.entity.SessionEventType as EntityType
import com.example.digitaldungeon.domain.model.SessionEventType as DomainType
import kotlinx.serialization.json.Json

fun SessionEventEntity.toDomain(): SessionEvent =
    SessionEvent(
        id = id,
        sessionId = sessionId,
        type = type.toDomain(),
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
        type = type.toEntity(),
        title = title,
        content = content,
        orderIndex = orderIndex,
        timestamp = timestamp,
        participants = if (participants.isEmpty()) null else Json.encodeToString(participants)
    )

fun EntityType.toDomain(): DomainType = when (this) {
    EntityType.SCENE -> DomainType.SCENE
    EntityType.COMBAT -> DomainType.COMBAT
    EntityType.DIALOGUE -> DomainType.DIALOGUE
    EntityType.DISCOVERY -> DomainType.DISCOVERY
    EntityType.NOTE -> DomainType.NOTE
}

fun DomainType.toEntity(): EntityType = when (this) {
    DomainType.SCENE -> EntityType.SCENE
    DomainType.COMBAT -> EntityType.COMBAT
    DomainType.DIALOGUE -> EntityType.DIALOGUE
    DomainType.DISCOVERY -> EntityType.DISCOVERY
    DomainType.NOTE -> EntityType.NOTE
}
