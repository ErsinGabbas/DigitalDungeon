package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.QuestHistoryRepository
import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [QuestHistoryRepository]; see [FakeCampaignRepository] for the
 * conventions.
 *
 * The shortest fake in the set, because the interface it stands in for is read-only: there is no
 * write for a test to record. Whether an entry was *appended* is asserted against the real
 * repositories that append it, which is where that logic lives.
 */
class FakeQuestHistoryRepository : QuestHistoryRepository {

    val entries = MutableStateFlow<List<QuestHistoryEntry>>(emptyList())

    var getHistoryForQuestError: Exception? = null

    val getHistoryForQuestCalls = mutableListOf<Int>()

    override fun getHistoryForQuest(questId: Int): Flow<List<QuestHistoryEntry>> {
        getHistoryForQuestCalls += questId
        return getHistoryForQuestError?.let { error -> flow<List<QuestHistoryEntry>> { throw error } }
            ?: entries
    }
}
