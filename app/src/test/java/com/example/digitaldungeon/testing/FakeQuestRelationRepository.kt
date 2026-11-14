package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.AddRelationResult
import com.example.digitaldungeon.data.repository.QuestRelationRepository
import com.example.digitaldungeon.domain.model.QuestRelation
import com.example.digitaldungeon.domain.model.QuestRelationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestRelationRepository]; see [FakeCampaignRepository] for the
 * conventions.
 *
 * Unlike the other fakes, [addResult] exists as well as [addRelationError]: the real repository
 * reports its four refusals as values and throws only when the write itself fails, so a test needs
 * to be able to set either. The recorded call carries the ends and the type exactly as they were
 * handed over, which is where a test sees whether the ViewModel swapped them - the normalisation of
 * `FOLLOWS` and of the symmetric types happens inside the real repository and is its own business.
 */
class FakeQuestRelationRepository : QuestRelationRepository {

    val relations = MutableStateFlow<List<QuestRelation>>(emptyList())

    var getRelationsForQuestError: Exception? = null
    var addRelationError: Exception? = null
    var deleteRelationError: Exception? = null

    /** What [addRelation] answers. Defaults to the link having been written. */
    var addResult: AddRelationResult = AddRelationResult.Added

    data class AddRelationCall(
        val sourceQuestId: Int,
        val targetQuestId: Int,
        val relationType: QuestRelationType
    )

    val addRelationCalls = mutableListOf<AddRelationCall>()
    val deleteRelationCalls = mutableListOf<Int>()
    val getRelationsForQuestCalls = mutableListOf<Int>()

    override fun getRelationsForQuest(questId: Int): Flow<List<QuestRelation>> {
        getRelationsForQuestCalls += questId
        return getRelationsForQuestError?.let { error -> flow<List<QuestRelation>> { throw error } }
            ?: relations
    }

    override suspend fun addRelation(
        sourceQuestId: Int,
        targetQuestId: Int,
        relationType: QuestRelationType
    ): AddRelationResult {
        addRelationCalls += AddRelationCall(sourceQuestId, targetQuestId, relationType)
        addRelationError?.let { throw it }
        return addResult
    }

    override suspend fun deleteRelation(id: Int) {
        deleteRelationCalls += id
        deleteRelationError?.let { throw it }
    }
}
