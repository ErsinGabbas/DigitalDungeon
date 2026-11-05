package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A participant that belongs to a campaign roster.
 *
 * Unlike the participant snapshots stored inside `session_events.participants`, these rows are
 * queryable: the roster can be searched, filtered and reused. [orderIndex] keeps the roster order
 * stable and reproducible, because a relation query gives no ordering guarantee of its own.
 */
@Entity(
    tableName = "campaign_participants",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["campaignId"])
    ]
)
data class CampaignParticipantEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val campaignId: Int,

    val name: String,

    val level: Int,

    val maxHp: Int,

    val orderIndex: Int
)
