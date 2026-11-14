package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestRewardType

/**
 * One reward row, owned by exactly one quest.
 *
 * A single index, on [questId]: it is the only column any query filters by, and the only ordering
 * the reads ask for is by [id], which the primary key already provides. The second index
 * `quest_objectives` carries pays for an `orderIndex` that has no counterpart here, so adding one
 * would be weight every insert pays for nothing.
 *
 * `CASCADE` on the quest, which itself cascades from its campaign: deleting a campaign takes its
 * quests and with them the rewards those quests promised, which is the right outcome - a reward
 * exists only as the end of a quest, and on its own it would say nothing.
 *
 * [type] is stored by Room as TEXT holding the constant name, exactly like `quests.status`, which
 * is why no `@TypeConverters` appear anywhere in this database.
 */
@Entity(
    tableName = "quest_rewards",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"])
    ]
)
data class QuestRewardEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val title: String,

    val description: String,

    val type: QuestRewardType
)
