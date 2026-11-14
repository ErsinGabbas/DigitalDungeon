package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestRewardRepository
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestRewardRepository]; see [FakeCampaignRepository] for the
 * conventions (state-flow driven emissions, recorded calls, injectable failures).
 *
 * The recorded calls carry every argument, because the write replaces the three fields as a whole:
 * "which values arrived here" is the only place a test can see that editing the title left the kind
 * alone.
 */
class FakeQuestRewardRepository : QuestRewardRepository {

    val rewards = MutableStateFlow<List<QuestReward>>(emptyList())

    var getRewardsForQuestError: Exception? = null
    var addRewardError: Exception? = null
    var updateRewardError: Exception? = null
    var deleteRewardError: Exception? = null

    data class AddRewardCall(
        val questId: Int,
        val title: String,
        val description: String,
        val type: QuestRewardType
    )

    data class UpdateRewardCall(
        val id: Int,
        val title: String,
        val description: String,
        val type: QuestRewardType
    )

    val addRewardCalls = mutableListOf<AddRewardCall>()
    val updateRewardCalls = mutableListOf<UpdateRewardCall>()
    val deleteRewardCalls = mutableListOf<Int>()
    val getRewardsForQuestCalls = mutableListOf<Int>()

    override fun getRewardsForQuest(questId: Int): Flow<List<QuestReward>> {
        getRewardsForQuestCalls += questId
        return getRewardsForQuestError?.let { error -> flow<List<QuestReward>> { throw error } }
            ?: rewards
    }

    override suspend fun addReward(
        questId: Int,
        title: String,
        description: String,
        type: QuestRewardType
    ) {
        addRewardCalls += AddRewardCall(questId, title, description, type)
        addRewardError?.let { throw it }
    }

    override suspend fun updateReward(
        id: Int,
        title: String,
        description: String,
        type: QuestRewardType
    ) {
        updateRewardCalls += UpdateRewardCall(id, title, description, type)
        updateRewardError?.let { throw it }
    }

    override suspend fun deleteReward(id: Int) {
        deleteRewardCalls += id
        deleteRewardError?.let { throw it }
    }
}
