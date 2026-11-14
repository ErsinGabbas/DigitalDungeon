package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestNoteRepository
import com.example.digitaldungeon.domain.model.QuestNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestNoteRepository]; see [FakeCampaignRepository] for the
 * conventions.
 *
 * No timestamps in the recorded calls, because the real repository does not take any: it stamps
 * `System.currentTimeMillis()` itself, so there is nothing for a caller to get wrong and nothing
 * here to record.
 */
class FakeQuestNoteRepository : QuestNoteRepository {

    val notes = MutableStateFlow<List<QuestNote>>(emptyList())

    var getNotesForQuestError: Exception? = null
    var addNoteError: Exception? = null
    var updateNoteError: Exception? = null
    var deleteNoteError: Exception? = null

    data class AddNoteCall(
        val questId: Int,
        val content: String
    )

    data class UpdateNoteCall(
        val id: Int,
        val content: String
    )

    val addNoteCalls = mutableListOf<AddNoteCall>()
    val updateNoteCalls = mutableListOf<UpdateNoteCall>()
    val deleteNoteCalls = mutableListOf<Int>()
    val getNotesForQuestCalls = mutableListOf<Int>()

    override fun getNotesForQuest(questId: Int): Flow<List<QuestNote>> {
        getNotesForQuestCalls += questId
        return getNotesForQuestError?.let { error -> flow<List<QuestNote>> { throw error } }
            ?: notes
    }

    override suspend fun addNote(questId: Int, content: String) {
        addNoteCalls += AddNoteCall(questId, content)
        addNoteError?.let { throw it }
    }

    override suspend fun updateNote(id: Int, content: String) {
        updateNoteCalls += UpdateNoteCall(id, content)
        updateNoteError?.let { throw it }
    }

    override suspend fun deleteNote(id: Int) {
        deleteNoteCalls += id
        deleteNoteError?.let { throw it }
    }
}
