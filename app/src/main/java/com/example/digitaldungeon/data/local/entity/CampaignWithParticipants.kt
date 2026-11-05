package com.example.digitaldungeon.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A campaign together with its roster, loaded by Room in a single `@Transaction` read.
 *
 * Room issues the relation query without an `ORDER BY`, so consumers must order by
 * [CampaignParticipantEntity.orderIndex] themselves - the mapper does that.
 */
data class CampaignWithParticipants(

    @Embedded
    val campaign: CampaignEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "campaignId"
    )
    val participants: List<CampaignParticipantEntity>
)
