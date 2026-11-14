package com.example.digitaldungeon.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One link row, pointing at a quest and a session.
 *
 * Two foreign keys into two different parents, and `CASCADE` on both, because deleting either the
 * quest or the session has to take the link with it - a link to something that is gone is exactly
 * the dangling reference this design refuses to allow.
 *
 * One index per end, because the link is read from both sides: the quest screen asks which sessions
 * a quest came up in, and the session screen asks which quests a session touched. Neither question
 * is the secondary one, so neither end can make do with a scan.
 *
 * The third index is the last word on duplicates. The repository checks before inserting so that
 * linking the same pair twice comes back as an answer rather than as an exception, but it is the
 * unique constraint that makes the second row impossible instead of merely unlikely.
 */
@Entity(
    tableName = "quest_session_links",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["sessionId"]),
        Index(value = ["questId", "sessionId"], unique = true)
    ]
)
data class QuestSessionLinkEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val questId: Int,

    val sessionId: Int
)
