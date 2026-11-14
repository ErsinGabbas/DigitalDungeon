package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One note row, owned by exactly one quest.
 *
 * A single index, on [questId], because that is the only column anything filters by: the notes are
 * always read as "the notes of this quest" and never searched or looked up by date, so a second
 * index would be a write cost nothing reads back. The ordering by `createdAt` rides along on the
 * small set of rows one quest has.
 *
 * `CASCADE` on the quest, which itself cascades from its campaign: deleting a campaign therefore
 * takes its quests and their notes, and none of it is left behind as rows nothing points at.
 *
 * Both timestamps are plain epoch millis columns, like everywhere else in this database - no
 * converter, nothing to keep in step between the two layers.
 */
@Entity(
    tableName = "quest_notes",
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
data class QuestNoteEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val content: String,

    val createdAt: Long,

    val updatedAt: Long
)
