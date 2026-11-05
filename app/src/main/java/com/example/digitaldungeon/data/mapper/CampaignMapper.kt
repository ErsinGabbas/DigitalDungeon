package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.CampaignWithParticipants
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
 *
 * Only event snapshots are still stored this way; campaign rosters live in `campaign_participants`.
 */
internal fun decodeParticipants(raw: String?): List<EventParticipant> =
    raw?.let {
        try {
            mapperJson.decodeFromString<List<EventParticipant>>(it)
        } catch (e: SerializationException) {
            emptyList()
        }
    } ?: emptyList()

/**
 * Room gives no ordering guarantee for a `@Relation`, so the roster is ordered here.
 */
fun CampaignWithParticipants.toDomain(): Campaign =
    Campaign(
        id = campaign.id,
        name = campaign.name,
        description = campaign.description,
        createdAt = campaign.createdAt,
        participants = participants
            .sortedBy { it.orderIndex }
            .map { it.toDomain() }
    )

fun CampaignParticipantEntity.toDomain(): EventParticipant =
    EventParticipant(
        name = name,
        level = level,
        maxHp = maxHp
    )

/**
 * Turns a roster into rows for [campaignId], using the list position as the stored order.
 */
fun List<EventParticipant>.toCampaignParticipantEntities(
    campaignId: Int
): List<CampaignParticipantEntity> =
    mapIndexed { index, participant ->
        CampaignParticipantEntity(
            campaignId = campaignId,
            name = participant.name,
            level = participant.level,
            maxHp = participant.maxHp,
            orderIndex = index
        )
    }

fun Campaign.toEntity() =
    CampaignEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )
