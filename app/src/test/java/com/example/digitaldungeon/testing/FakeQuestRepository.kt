package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestRepository]; see [FakeCampaignRepository] for the conventions
 * (state-flow driven emissions, recorded calls, injectable failures).
 *
 * The recorded calls carry every argument, because the whole point of the form is that it writes
 * the fields it showed and nothing else: "which values arrived here" is the only place a test can
 * see that editing a title left the priority alone.
 *
 * Note: the defaults of [addQuest] live on the interface, so the override here deliberately
 * declares none - which is also what lets a test assert what the defaults actually were.
 */
class FakeQuestRepository : QuestRepository {

    val quests = MutableStateFlow<List<Quest>>(emptyList())

    val questById = MutableStateFlow<Quest?>(null)

    var getQuestsForCampaignError: Exception? = null
    var getQuestByIdError: Exception? = null
    var addQuestError: Exception? = null
    var updateQuestError: Exception? = null
    var updateQuestStatusError: Exception? = null
    var deleteQuestError: Exception? = null

    data class AddQuestCall(
        val campaignId: Int,
        val title: String,
        val description: String,
        val category: QuestCategory,
        val status: QuestStatus,
        val priority: QuestPriority,
        val isHidden: Boolean,
        val inGameDate: String?
    )

    data class UpdateQuestCall(
        val id: Int,
        val title: String,
        val description: String,
        val category: QuestCategory,
        val status: QuestStatus,
        val priority: QuestPriority,
        val isHidden: Boolean,
        val inGameDate: String?
    )

    data class UpdateStatusCall(
        val id: Int,
        val status: QuestStatus
    )

    val addQuestCalls = mutableListOf<AddQuestCall>()
    val updateQuestCalls = mutableListOf<UpdateQuestCall>()
    val updateStatusCalls = mutableListOf<UpdateStatusCall>()
    val deleteQuestCalls = mutableListOf<Int>()
    val getQuestsForCampaignCalls = mutableListOf<Int>()
    val getQuestByIdCalls = mutableListOf<Int>()

    override fun getQuestsForCampaign(campaignId: Int): Flow<List<Quest>> {
        getQuestsForCampaignCalls += campaignId
        return getQuestsForCampaignError?.let { error -> flow<List<Quest>> { throw error } }
            ?: quests
    }

    override fun getQuestById(id: Int): Flow<Quest?> {
        getQuestByIdCalls += id
        return getQuestByIdError?.let { error -> flow<Quest?> { throw error } }
            ?: questById
    }

    override suspend fun addQuest(
        campaignId: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    ) {
        addQuestCalls += AddQuestCall(
            campaignId = campaignId,
            title = title,
            description = description,
            category = category,
            status = status,
            priority = priority,
            isHidden = isHidden,
            inGameDate = inGameDate
        )
        addQuestError?.let { throw it }
    }

    override suspend fun updateQuest(
        id: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    ) {
        updateQuestCalls += UpdateQuestCall(
            id = id,
            title = title,
            description = description,
            category = category,
            status = status,
            priority = priority,
            isHidden = isHidden,
            inGameDate = inGameDate
        )
        updateQuestError?.let { throw it }
    }

    override suspend fun updateQuestStatus(id: Int, status: QuestStatus) {
        updateStatusCalls += UpdateStatusCall(id, status)
        updateQuestStatusError?.let { throw it }
    }

    override suspend fun deleteQuest(id: Int) {
        deleteQuestCalls += id
        deleteQuestError?.let { throw it }
    }
}
