package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.AddSessionLinkResult
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepository
import com.example.digitaldungeon.domain.model.QuestSessionLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestSessionLinkRepository]; see [FakeCampaignRepository] for the
 * conventions and [FakeQuestRelationRepository] for the sibling that also answers with a result
 * type.
 *
 * Two flows, because the real table is read from both ends: the quest screen asks which sessions,
 * the session screen asks which quests.
 */
class FakeQuestSessionLinkRepository : QuestSessionLinkRepository {

    val linksForQuest = MutableStateFlow<List<QuestSessionLink>>(emptyList())
    val linksForSession = MutableStateFlow<List<QuestSessionLink>>(emptyList())

    var getLinksForQuestError: Exception? = null
    var getLinksForSessionError: Exception? = null
    var addLinkError: Exception? = null
    var deleteLinkError: Exception? = null

    /** What [addLink] answers. Defaults to the link having been written. */
    var addResult: AddSessionLinkResult = AddSessionLinkResult.Added

    data class AddLinkCall(
        val questId: Int,
        val sessionId: Int
    )

    val addLinkCalls = mutableListOf<AddLinkCall>()
    val deleteLinkCalls = mutableListOf<Int>()
    val getLinksForQuestCalls = mutableListOf<Int>()
    val getLinksForSessionCalls = mutableListOf<Int>()

    override fun getLinksForQuest(questId: Int): Flow<List<QuestSessionLink>> {
        getLinksForQuestCalls += questId
        return getLinksForQuestError?.let { error ->
            flow<List<QuestSessionLink>> { throw error }
        } ?: linksForQuest
    }

    override fun getLinksForSession(sessionId: Int): Flow<List<QuestSessionLink>> {
        getLinksForSessionCalls += sessionId
        return getLinksForSessionError?.let { error ->
            flow<List<QuestSessionLink>> { throw error }
        } ?: linksForSession
    }

    override suspend fun addLink(questId: Int, sessionId: Int): AddSessionLinkResult {
        addLinkCalls += AddLinkCall(questId, sessionId)
        addLinkError?.let { throw it }
        return addResult
    }

    override suspend fun deleteLink(id: Int) {
        deleteLinkCalls += id
        deleteLinkError?.let { throw it }
    }
}
