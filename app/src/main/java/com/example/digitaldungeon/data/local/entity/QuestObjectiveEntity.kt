package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus

/**
 * One objective row, owned by exactly one quest.
 *
 * Two indices, both earned by a query: [questId] is what every read filters by, and
 * `(questId, orderIndex)` is what every read orders by - the same pair `session_events` carries,
 * for the same reason.
 *
 * `CASCADE` on the quest, which itself cascades from its campaign: deleting a campaign therefore
 * takes its quests and their objectives, and none of it is left behind as rows nothing points at.
 */
@Entity(
    tableName = "quest_objectives",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["questId", "orderIndex"])
    ]
)
data class QuestObjectiveEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val title: String,

    val description: String,

    val status: QuestObjectiveStatus,

    val orderIndex: Int,

    val isOptional: Boolean
)
