package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "orderIndex"])
    ]
)
data class SessionEventEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val sessionId: Int,

    val type: SessionEventType,

    val title: String?,

    val content: String,

    val orderIndex: Int,

    val timestamp: Long
)

enum class SessionEventType {
    SCENE,
    COMBAT,
    DIALOGUE,
    DISCOVERY,
    NOTE
}
