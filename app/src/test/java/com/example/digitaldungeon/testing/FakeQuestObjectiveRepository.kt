package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestObjectiveRepository
import com.example.digitaldungeon.data.repository.QuestObjectiveRepository.MoveDirection
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestObjectiveRepository]; see [FakeCampaignRepository] for the
 * conventions (state-flow driven emissions, recorded calls, injectable failures).
 *
 * The recorded calls carry every argument. For the form that matters as much as it does for the
 * quest form: `updateObjective` writes title, details and the optional flag as a whole, so "which
 * values arrived here" is the only place a test can see that editing the text left the flag alone.
 *
 * Note: the defaults of [addObjective] live on the interface, so the override here deliberately
 * declares none - which is also what lets a test assert what the defaults actually were.
 */
class FakeQuestObjectiveRepository : QuestObjectiveRepository {

    val objectives = MutableStateFlow<List<QuestObjective>>(emptyList())

    var getObjectivesForQuestError: Exception? = null
    var addObjectiveError: Exception? = null
    var updateObjectiveError: Exception? = null
    var updateObjectiveStatusError: Exception? = null
    var moveObjectiveError: Exception? = null
    var deleteObjectiveError: Exception? = null

    data class AddObjectiveCall(
        val questId: Int,
        val title: String,
        val description: String,
        val isOptional: Boolean
    )

    data class UpdateObjectiveCall(
        val id: Int,
        val title: String,
        val description: String,
        val isOptional: Boolean
    )

    data class UpdateStatusCall(
        val id: Int,
        val status: QuestObjectiveStatus
    )

    data class MoveObjectiveCall(
        val id: Int,
        val direction: MoveDirection
    )

    val addObjectiveCalls = mutableListOf<AddObjectiveCall>()
    val updateObjectiveCalls = mutableListOf<UpdateObjectiveCall>()
    val updateStatusCalls = mutableListOf<UpdateStatusCall>()
    val moveObjectiveCalls = mutableListOf<MoveObjectiveCall>()
    val deleteObjectiveCalls = mutableListOf<Int>()
    val getObjectivesForQuestCalls = mutableListOf<Int>()

    override fun getObjectivesForQuest(questId: Int): Flow<List<QuestObjective>> {
        getObjectivesForQuestCalls += questId
        return getObjectivesForQuestError?.let { error ->
            flow<List<QuestObjective>> { throw error }
        } ?: objectives
    }

    override suspend fun addObjective(
        questId: Int,
        title: String,
        description: String,
        isOptional: Boolean
    ) {
        addObjectiveCalls += AddObjectiveCall(questId, title, description, isOptional)
        addObjectiveError?.let { throw it }
    }

    override suspend fun updateObjective(
        id: Int,
        title: String,
        description: String,
        isOptional: Boolean
    ) {
        updateObjectiveCalls += UpdateObjectiveCall(id, title, description, isOptional)
        updateObjectiveError?.let { throw it }
    }

    override suspend fun updateObjectiveStatus(id: Int, status: QuestObjectiveStatus) {
        updateStatusCalls += UpdateStatusCall(id, status)
        updateObjectiveStatusError?.let { throw it }
    }

    override suspend fun moveObjective(id: Int, direction: MoveDirection) {
        moveObjectiveCalls += MoveObjectiveCall(id, direction)
        moveObjectiveError?.let { throw it }
    }

    override suspend fun deleteObjective(id: Int) {
        deleteObjectiveCalls += id
        deleteObjectiveError?.let { throw it }
    }
}
