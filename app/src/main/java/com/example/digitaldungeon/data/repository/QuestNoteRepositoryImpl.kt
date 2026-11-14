package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.QuestNoteDao
import com.example.digitaldungeon.data.local.entity.QuestNoteEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestNoteRepositoryImpl @Inject constructor(
    private val dao: QuestNoteDao
) : QuestNoteRepository {

    override fun getNotesForQuest(
        questId: Int
    ): Flow<List<QuestNote>> =
        dao.getNotesForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addNote(
        questId: Int,
        content: String
    ) {

        // Read once and used for both, so a fresh note has the two timestamps exactly equal
        // instead of a millisecond apart - that equality is how an unedited note is told apart
        // from a revised one.
        val now = System.currentTimeMillis()

        dao.insertNote(
            QuestNoteEntity(
                questId = questId,
                content = content,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateNote(
        id: Int,
        content: String
    ) {

        dao.updateNote(
            id = id,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteNote(
        id: Int
    ) {

        dao.deleteNote(id)

    }
}
