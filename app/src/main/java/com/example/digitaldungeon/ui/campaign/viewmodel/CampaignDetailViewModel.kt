package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.ui.campaign.state.CampaignDetailUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignParticipantsUiState
import com.example.digitaldungeon.ui.session.state.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    // Campaign state - owns only campaign loading errors
    private val _campaignUiState =
        MutableStateFlow(CampaignDetailUiState())

    val campaignUiState =
        _campaignUiState.asStateFlow()

    // Session state - owns only session errors (load / add / delete)
    private val _sessionUiState =
        MutableStateFlow(SessionUiState())

    val sessionUiState =
        _sessionUiState.asStateFlow()

    // Participant management state - owns only participant saving errors
    private val _participantsUiState =
        MutableStateFlow(CampaignParticipantsUiState())

    val participantsUiState =
        _participantsUiState.asStateFlow()

    private var loadSessionsJob: Job? = null

    private var loadCampaignJob: Job? = null

    fun loadCampaign(id: Int) {

        loadCampaignJob?.cancel()

        loadCampaignJob = viewModelScope.launch {

            try {
                _campaignUiState.update { it.copy(isLoading = true) }
                campaignRepository
                    .getCampaignByIdFlow(id)
                    .collect { campaign ->

                        onCampaignParticipantsLoaded(
                            campaign?.participants ?: emptyList()
                        )

                        _campaignUiState.update {
                            it.copy(
                                campaign = campaign,
                                isLoading = false,
                                error = if (campaign == null) "Campaign not found" else null
                            )
                        }
                    }
            } catch (e: Exception) {
                _campaignUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load campaign"
                    )
                }
            }

        }
    }

    /**
     * Keeps the editable collection in sync with the database without throwing away
     * edits the user has not saved yet.
     */
    private fun onCampaignParticipantsLoaded(
        participants: List<EventParticipant>
    ) {

        _participantsUiState.update { current ->
            if (current.hasUnsavedChanges) {
                current.copy(savedParticipants = participants)
            } else {
                current.copy(
                    participants = participants,
                    savedParticipants = participants
                )
            }
        }
    }

    fun loadSessions(
        campaignId: Int
    ) {

        loadSessionsJob?.cancel()

        loadSessionsJob = viewModelScope.launch {

            try {
                _sessionUiState.update { it.copy(isLoading = true) }
                sessionRepository
                    .getSessionsForCampaign(campaignId)
                    .collect { sessions ->

                        _sessionUiState.update {
                            it.copy(
                                sessions = sessions,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sessions"
                    )
                }
            }
        }
    }

    fun onSessionTitleChange(
        value: String
    ) {

        _sessionUiState.update {
            it.copy(
                sessionTitle = value
            )
        }
    }

    fun onSessionNotesChange(
        value: String
    ) {

        _sessionUiState.update {
            it.copy(
                sessionNotes = value
            )
        }
    }

    fun addSession(
        campaignId: Int
    ) {

        val title =
            _sessionUiState.value.sessionTitle

        val notes =
            _sessionUiState.value.sessionNotes

        if (title.isBlank()) return

        viewModelScope.launch {

            try {
                _sessionUiState.update { it.copy(isLoading = true) }
                sessionRepository.addSession(
                    campaignId = campaignId,
                    title = title,
                    notes = notes
                )

                _sessionUiState.update {
                    it.copy(
                        sessionTitle = "",
                        sessionNotes = "",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to add session"
                    )
                }
            }
        }
    }

    fun deleteSession(
        id: Int
    ) {

        viewModelScope.launch {

            try {
                _sessionUiState.update { it.copy(isLoading = true) }
                sessionRepository.deleteSession(id)
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete session"
                    )
                }
            }

        }
    }

    // Participant management methods
    fun onParticipantNameChange(value: String) {
        _participantsUiState.update {
            it.copy(participantName = value)
        }
    }

    fun onParticipantLevelChange(value: String) {
        _participantsUiState.update {
            it.copy(participantLevel = value)
        }
    }

    fun onParticipantMaxHpChange(value: String) {
        _participantsUiState.update {
            it.copy(participantMaxHp = value)
        }
    }

    fun addParticipant() {

        _participantsUiState.update { state ->

            val participant =
                state.toParticipantOrNull() ?: return@update state

            state.copy(
                participants = state.participants + participant
            ).clearForm()
        }
    }

    fun editParticipant(index: Int) {

        _participantsUiState.update { state ->

            val participant =
                state.participants.getOrNull(index) ?: return@update state

            state.copy(
                editingIndex = index,
                participantName = participant.name,
                participantLevel = participant.level.toString(),
                participantMaxHp = participant.maxHp.toString()
            )
        }
    }

    fun updateParticipant() {

        _participantsUiState.update { state ->

            val index =
                state.editingIndex ?: return@update state

            if (index !in state.participants.indices) return@update state

            val participant =
                state.toParticipantOrNull() ?: return@update state

            state.copy(
                participants = state.participants.mapIndexed { i, existing ->
                    if (i == index) participant else existing
                }
            ).clearForm()
        }
    }

    fun removeParticipant(index: Int) {

        _participantsUiState.update { state ->

            if (index !in state.participants.indices) return@update state

            val updated =
                state.participants.filterIndexed { i, _ -> i != index }

            val editingIndex = state.editingIndex

            if (editingIndex == null) {
                state.copy(participants = updated)
            } else if (editingIndex == index) {
                // The row being edited is gone - drop the edit form as well.
                state.copy(participants = updated).clearForm()
            } else {
                // Keep editing the same row, its position may have shifted.
                state.copy(
                    participants = updated,
                    editingIndex = if (index < editingIndex) editingIndex - 1 else editingIndex
                )
            }
        }
    }

    fun cancelEdit() {
        _participantsUiState.update { it.clearForm() }
    }

    fun saveParticipants(campaignId: Int) {

        viewModelScope.launch {

            val participants =
                _participantsUiState.value.participants

            try {
                _participantsUiState.update {
                    it.copy(isSaving = true)
                }

                campaignRepository.updateCampaignParticipants(
                    campaignId = campaignId,
                    participants = participants
                )

                // getCampaignByIdFlow pushes the stored value back, this only marks
                // the collection as clean right away.
                _participantsUiState.update {
                    it.copy(
                        savedParticipants = participants,
                        isSaving = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _participantsUiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save participants"
                    )
                }
            }
        }
    }

    private fun CampaignParticipantsUiState.toParticipantOrNull(): EventParticipant? {

        val name = participantName.trim()
        val level = participantLevel.toIntOrNull()
        val maxHp = participantMaxHp.toIntOrNull()

        if (name.isBlank() || level == null || maxHp == null) return null

        return EventParticipant(
            name = name,
            level = level,
            maxHp = maxHp
        )
    }

    private fun CampaignParticipantsUiState.clearForm() =
        copy(
            participantName = "",
            participantLevel = "",
            participantMaxHp = "",
            editingIndex = null
        )
}
