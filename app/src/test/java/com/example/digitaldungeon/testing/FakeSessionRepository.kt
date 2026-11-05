package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [SessionRepository]; see [FakeCampaignRepository] for the
 * conventions (state-flow driven emissions, recorded calls, injectable failures).
 */
class FakeSessionRepository : SessionRepository {

    val sessions = MutableStateFlow<List<Session>>(emptyList())

    val session = MutableStateFlow<Session?>(null)

    var getSessionsForCampaignError: Exception? = null
    var getSessionByIdError: Exception? = null
    var addSessionError: Exception? = null
    var updateSessionError: Exception? = null
    var deleteSessionError: Exception? = null

    data class AddSessionCall(
        val campaignId: Int,
        val title: String,
        val notes: String
    )

    data class UpdateSessionCall(
        val id: Int,
        val title: String,
        val notes: String
    )

    val addSessionCalls = mutableListOf<AddSessionCall>()
    val updateSessionCalls = mutableListOf<UpdateSessionCall>()
    val deleteSessionCalls = mutableListOf<Int>()
    val getSessionsForCampaignCalls = mutableListOf<Int>()
    val getSessionByIdCalls = mutableListOf<Int>()

    override fun getSessionsForCampaign(campaignId: Int): Flow<List<Session>> {
        getSessionsForCampaignCalls += campaignId
        return getSessionsForCampaignError?.let { error -> flow<List<Session>> { throw error } }
            ?: sessions
    }

    override fun getSessionById(id: Int): Flow<Session?> {
        getSessionByIdCalls += id
        return getSessionByIdError?.let { error -> flow<Session?> { throw error } } ?: session
    }

    override suspend fun addSession(campaignId: Int, title: String, notes: String) {
        addSessionCalls += AddSessionCall(campaignId, title, notes)
        addSessionError?.let { throw it }
    }

    override suspend fun updateSession(id: Int, title: String, notes: String) {
        updateSessionCalls += UpdateSessionCall(id, title, notes)
        updateSessionError?.let { throw it }
    }

    override suspend fun deleteSession(id: Int) {
        deleteSessionCalls += id
        deleteSessionError?.let { throw it }
    }
}
