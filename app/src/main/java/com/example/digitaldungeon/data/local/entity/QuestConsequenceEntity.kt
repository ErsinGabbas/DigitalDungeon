package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestConsequenceType

/**
 * One consequence row, owned by exactly one quest.
 *
 * A single index, on [questId]: it is the only column any query filters by, and the only ordering
 * the reads ask for is by [id], which the primary key already provides. The composite index
 * `quest_objectives` carries buys an `orderIndex` that has no counterpart here, so a second index
 * would be weight every insert pays for nothing.
 *
 * `CASCADE` on the quest, which itself cascades from its campaign. A consequence does read as
 * something that outlived its quest, but it is still a thing that quest caused and says nothing
 * once the quest is gone; deleting the campaign is the DM throwing the whole story away, and
 * keeping its aftermath around as rows nothing points at would only be a leak.
 *
 * [type] is stored by Room as TEXT holding the constant name, exactly like `quests.status`, which
 * is why no `@TypeConverters` appear anywhere in this database.
 */
@Entity(
    tableName = "quest_consequences",
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
data class QuestConsequenceEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val title: String,

    val description: String,

    val type: QuestConsequenceType
)
