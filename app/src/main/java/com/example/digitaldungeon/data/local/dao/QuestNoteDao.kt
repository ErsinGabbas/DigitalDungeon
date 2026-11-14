package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestNoteDao {

    /**
     * The notes of one quest, newest first - the most recent thought is the one being worked with,
     * so it is the one that should not need scrolling to.
     *
     * Tie-broken by id, so two notes written inside the same millisecond still come out in a fixed
     * order instead of whatever the query planner felt like - the same guarantee
     * `getObjectivesForQuest` makes about the objective list.
     */
    @Query(
        "SELECT * FROM quest_notes WHERE questId = :questId ORDER BY createdAt DESC, id DESC"
    )
    fun getNotesForQuest(
        questId: Int
    ): Flow<List<QuestNoteEntity>>

    @Insert
    suspend fun insertNote(
        note: QuestNoteEntity
    )

    /**
     * Every note of every quest, for a full backup. Ordered so the produced document is stable:
     * the same database always exports byte-identical content. By id rather than by `createdAt`,
     * because the export is about the rows and not about reading order.
     */
    @Query(
        "SELECT * FROM quest_notes ORDER BY questId ASC, id ASC"
    )
    suspend fun getAllNotes(): List<QuestNoteEntity>

    @Insert
    suspend fun insertNotes(
        notes: List<QuestNoteEntity>
    )

    @Query(
        "DELETE FROM quest_notes"
    )
    suspend fun deleteAllNotes()

    /**
     * Rewrites what the note says.
     *
     * `createdAt` is deliberately absent: when the thought was written down is a fact about the
     * past, and an edit is not allowed to rewrite it - that is also what keeps a reworded note in
     * its place in the list.
     */
    @Query(
        "UPDATE quest_notes SET content = :content, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateNote(
        id: Int,
        content: String,
        updatedAt: Long
    )

    @Query(
        "DELETE FROM quest_notes WHERE id = :id"
    )
    suspend fun deleteNote(
        id: Int
    )
}
