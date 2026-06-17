package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
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
data class SessionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val campaignId: Int,

    val title: String,

    val notes: String,

    val createdAt: Long
)
