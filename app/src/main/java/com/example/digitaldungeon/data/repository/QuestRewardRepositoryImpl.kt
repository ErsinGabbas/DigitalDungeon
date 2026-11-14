package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.QuestRewardDao
import com.example.digitaldungeon.data.local.entity.QuestRewardEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestRewardRepositoryImpl @Inject constructor(
    private val dao: QuestRewardDao
) : QuestRewardRepository {

    override fun getRewardsForQuest(
        questId: Int
    ): Flow<List<QuestReward>> =
        dao.getRewardsForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addReward(
        questId: Int,
        title: String,
        description: String,
        type: QuestRewardType
    ) {

        // No position to work out and nothing to read back first, unlike adding an objective: the
        // id the insert hands out is the order, so this is one statement and cannot race with a
        // second quick addition.
        dao.insertReward(
            QuestRewardEntity(
                questId = questId,
                title = title,
                description = description,
                type = type
            )
        )
    }

    override suspend fun updateReward(
        id: Int,
        title: String,
        description: String,
        type: QuestRewardType
    ) {

        dao.updateReward(
            id = id,
            title = title,
            description = description,
            type = type
        )
    }

    override suspend fun deleteReward(
        id: Int
    ) {

        dao.deleteReward(id)

    }
}
