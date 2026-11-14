package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestConsequenceRepository
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestConsequenceRepository]; see [FakeCampaignRepository] for the
 * conventions and [FakeQuestRewardRepository] for the near-identical sibling.
 */
class FakeQuestConsequenceRepository : QuestConsequenceRepository {

    val consequences = MutableStateFlow<List<QuestConsequence>>(emptyList())

    var getConsequencesForQuestError: Exception? = null
    var addConsequenceError: Exception? = null
    var updateConsequenceError: Exception? = null
    var deleteConsequenceError: Exception? = null

    data class AddConsequenceCall(
        val questId: Int,
        val title: String,
        val description: String,
        val type: QuestConsequenceType
    )

    data class UpdateConsequenceCall(
        val id: Int,
        val title: String,
        val description: String,
        val type: QuestConsequenceType
    )

    val addConsequenceCalls = mutableListOf<AddConsequenceCall>()
    val updateConsequenceCalls = mutableListOf<UpdateConsequenceCall>()
    val deleteConsequenceCalls = mutableListOf<Int>()
    val getConsequencesForQuestCalls = mutableListOf<Int>()

    override fun getConsequencesForQuest(questId: Int): Flow<List<QuestConsequence>> {
        getConsequencesForQuestCalls += questId
        return getConsequencesForQuestError?.let { error ->
            flow<List<QuestConsequence>> { throw error }
        } ?: consequences
    }

    override suspend fun addConsequence(
        questId: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    ) {
        addConsequenceCalls += AddConsequenceCall(questId, title, description, type)
        addConsequenceError?.let { throw it }
    }

    override suspend fun updateConsequence(
        id: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    ) {
        updateConsequenceCalls += UpdateConsequenceCall(id, title, description, type)
        updateConsequenceError?.let { throw it }
    }

    override suspend fun deleteConsequence(id: Int) {
        deleteConsequenceCalls += id
        deleteConsequenceError?.let { throw it }
    }
}
