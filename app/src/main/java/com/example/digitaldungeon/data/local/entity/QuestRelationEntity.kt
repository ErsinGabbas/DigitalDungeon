package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.digitaldungeon.domain.model.QuestRelationType

/**
 * One link row, pointing at two quests.
 *
 * Two foreign keys because the row has two ends, and `CASCADE` on both because deleting either end
 * has to take the row with it - a relation pointing at a quest that no longer exists is exactly
 * the dangling reference this design refuses to keep.
 *
 * One index per end rather than one covering both, because reads come from both directions: what a
 * quest points at, and what points at it. The third index is the last word on duplicates - the
 * repository checks before inserting and reports a duplicate as an ordinary outcome rather than a
 * failure, but it is the unique constraint that makes a duplicate impossible instead of merely
 * unlikely.
 */
@Entity(
    tableName = "quest_relations",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceQuestId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetQuestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceQuestId"]),
        Index(value = ["targetQuestId"]),
        Index(value = ["sourceQuestId", "targetQuestId", "relationType"], unique = true)
    ]
)
data class QuestRelationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val sourceQuestId: Int,

    val targetQuestId: Int,

    val relationType: QuestRelationType
)
