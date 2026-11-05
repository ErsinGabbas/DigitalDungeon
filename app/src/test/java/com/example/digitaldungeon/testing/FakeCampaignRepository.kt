package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Hand written stand-in for [CampaignRepository].
 *
 * Emissions are driven through the [campaigns] / [campaignById] state flows, every call
 * is recorded with its arguments, and any member can be made to fail by setting the
 * matching `...Error` property. A recorded call is appended *before* the failure is
 * thrown, so a test can tell "the ViewModel never called the repository" apart from
 * "the repository was called and refused".
 *
 * Note: the `participants` default of [addCampaign] lives on the interface, so the
 * override here deliberately declares none.
 */
class FakeCampaignRepository : CampaignRepository {

    val campaigns = MutableStateFlow<List<Campaign>>(emptyList())

    val campaignById = MutableStateFlow<Campaign?>(null)

    var getCampaignsError: Exception? = null
    var getCampaignByIdFlowError: Exception? = null
    var addCampaignError: Exception? = null
    var deleteCampaignError: Exception? = null
    var updateCampaignParticipantsError: Exception? = null

    data class AddCampaignCall(
        val name: String,
        val description: String,
        val participants: List<EventParticipant>
    )

    data class UpdateParticipantsCall(
        val campaignId: Int,
        val participants: List<EventParticipant>
    )

    val addCampaignCalls = mutableListOf<AddCampaignCall>()
    val deleteCampaignCalls = mutableListOf<Int>()
    val updateCampaignParticipantsCalls = mutableListOf<UpdateParticipantsCall>()
    val getCampaignByIdFlowCalls = mutableListOf<Int>()
    val getCampaignByIdCalls = mutableListOf<Int>()

    override fun getCampaigns(): Flow<List<Campaign>> =
        getCampaignsError?.let { error -> flow<List<Campaign>> { throw error } } ?: campaigns

    override suspend fun getCampaignById(id: Int): Campaign? {
        getCampaignByIdCalls += id
        return campaignById.value
    }

    override fun getCampaignByIdFlow(id: Int): Flow<Campaign?> {
        getCampaignByIdFlowCalls += id
        return getCampaignByIdFlowError?.let { error -> flow<Campaign?> { throw error } }
            ?: campaignById
    }

    override suspend fun addCampaign(
        name: String,
        description: String,
        participants: List<EventParticipant>
    ) {
        addCampaignCalls += AddCampaignCall(name, description, participants)
        addCampaignError?.let { throw it }
    }

    override suspend fun deleteCampaign(id: Int) {
        deleteCampaignCalls += id
        deleteCampaignError?.let { throw it }
    }

    override suspend fun updateCampaignParticipants(
        campaignId: Int,
        participants: List<EventParticipant>
    ) {
        updateCampaignParticipantsCalls += UpdateParticipantsCall(campaignId, participants)
        updateCampaignParticipantsError?.let { throw it }
    }
}
