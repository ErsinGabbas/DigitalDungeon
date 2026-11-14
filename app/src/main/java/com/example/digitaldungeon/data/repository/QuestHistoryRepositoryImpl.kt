package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestHistoryRepositoryImpl @Inject constructor(
    private val dao: QuestHistoryDao
) : QuestHistoryRepository {

    override fun getHistoryForQuest(
        questId: Int
    ): Flow<List<QuestHistoryEntry>> =
        dao.getHistoryForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }
}
