package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Shared Json instance for the mappers in this package.
 */
internal val mapperJson = Json { ignoreUnknownKeys = true }

/**
 * Decodes a stored participants payload. Broken or incompatible JSON must not
 * propagate out of a Flow.map, so a failed decode degrades to an empty list.
 */
internal fun decodeParticipants(raw: String?): List<EventParticipant> =
    raw?.let {
        try {
            mapperJson.decodeFromString<List<EventParticipant>>(it)
        } catch (e: SerializationException) {
            emptyList()
        }
    } ?: emptyList()

fun CampaignEntity.toDomain(): Campaign =
    Campaign(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = decodeParticipants(participants)
    )

fun Campaign.toEntity() =
    CampaignEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = if (participants.isEmpty()) null else Json.encodeToString(participants)
    )
