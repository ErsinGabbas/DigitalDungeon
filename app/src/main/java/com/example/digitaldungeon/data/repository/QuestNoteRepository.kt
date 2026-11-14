package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestNote
import kotlinx.coroutines.flow.Flow

/**
 * The DM's running notes on a quest.
 *
 * A repository of its own rather than more methods on [QuestRepository], the same way the
 * objectives are separate from the quest: the quest screen subscribes to each block
 * independently, so one block failing to load cannot take the quest down with it, and the notes
 * change for entirely different reasons than the quest they hang off.
 *
 * None of these take a timestamp. The repository reads the clock itself, because a caller that
 * passed its own could write a note into the past - a stale value held by a screen, a test fixture
 * that leaked, a device whose clock was wound back - and the order the notes are read in is the
 * only structure they have. The UI has no business knowing the time either: it hands over the text
 * the DM typed and nothing more.
 */
interface QuestNoteRepository {

    fun getNotesForQuest(
        questId: Int
    ): Flow<List<QuestNote>>

    /**
     * Writes a new note on [questId].
     *
     * Both timestamps are stamped now and to the same value, which is what makes an untouched note
     * recognisable: they stay equal until the first edit.
     */
    suspend fun addNote(
        questId: Int,
        content: String
    )

    /**
     * Rewrites what a note says and stamps the edit.
     *
     * Only `updatedAt` moves; when the thought was first written down is a fact about the past and
     * stays as it is, so a reworded note keeps its place in the list.
     */
    suspend fun updateNote(
        id: Int,
        content: String
    )

    suspend fun deleteNote(
        id: Int
    )
}
