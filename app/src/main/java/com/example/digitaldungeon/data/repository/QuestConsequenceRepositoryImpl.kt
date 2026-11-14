package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.QuestConsequenceDao
import com.example.digitaldungeon.data.local.entity.QuestConsequenceEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestConsequenceRepositoryImpl @Inject constructor(
    private val dao: QuestConsequenceDao
) : QuestConsequenceRepository {

    override fun getConsequencesForQuest(
        questId: Int
    ): Flow<List<QuestConsequence>> =
        dao.getConsequencesForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addConsequence(
        questId: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    ) {

        // No position to work out: the row's own id is the order, so nothing has to be read back
        // before the insert and two quick additions cannot collide.
        dao.insertConsequence(
            QuestConsequenceEntity(
                questId = questId,
                title = title,
                description = description,
                type = type
            )
        )
    }

    override suspend fun updateConsequence(
        id: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    ) {

        dao.updateConsequence(
            id = id,
            title = title,
            description = description,
            type = type
        )
    }

    override suspend fun deleteConsequence(
        id: Int
    ) {

        dao.deleteConsequence(id)

    }
}
