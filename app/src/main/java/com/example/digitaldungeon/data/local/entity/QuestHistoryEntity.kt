package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestHistoryEventType

/**
 * One history row, owned by exactly one quest.
 *
 * Two indices, both earned by the read: it filters by [questId] and orders by [timestamp], so
 * `(questId, timestamp)` is the pair SQLite can walk straight down instead of sorting the matches
 * afterwards. `quest_notes` and `quest_rewards` carry only the single [questId] index precisely
 * because nothing orders them - their rows come out however they come out - and this table is the
 * one place where the order is the content, since a log read out of sequence says nothing.
 *
 * `CASCADE` on the quest, which itself cascades from its campaign: deleting a campaign therefore
 * takes its quests and their history. That is also the only way a row ever leaves this table - the
 * log is append-only, so there is no delete to write anywhere else.
 *
 * [timestamp] is a plain epoch millis column, like everywhere else in this database - no converter,
 * nothing to keep in step between the two layers.
 */
@Entity(
    tableName = "quest_history",
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
        Index(value = ["questId", "timestamp"])
    ]
)
data class QuestHistoryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val eventType: QuestHistoryEventType,

    val description: String,

    val timestamp: Long
)
