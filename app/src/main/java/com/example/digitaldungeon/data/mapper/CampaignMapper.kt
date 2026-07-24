package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.serialization.json.Json

fun CampaignEntity.toDomain(): Campaign =
    Campaign(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = participants?.let { 
            Json.decodeFromString<List<EventParticipant>>(it)
        } ?: emptyList()
    )

fun Campaign.toEntity() =
    CampaignEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        participants = if (participants.isEmpty()) null else Json.encodeToString(participants)
    )