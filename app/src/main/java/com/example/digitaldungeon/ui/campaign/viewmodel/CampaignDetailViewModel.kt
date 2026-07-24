package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.ui.session.state.SessionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _campaign =
        MutableStateFlow<Campaign?>(null)

    val campaign =
        _campaign.asStateFlow()

    private val _sessionUiState =
        MutableStateFlow(SessionUiState())

    val sessionUiState =
        _sessionUiState.asStateFlow()

    // Participant management state
    private val _participantName = MutableStateFlow("")
    val participantName = _participantName.asStateFlow()

    private val _participantLevel = MutableStateFlow("")
    val participantLevel = _participantLevel.asStateFlow()

    private val _participantMaxHp = MutableStateFlow("")
    val participantMaxHp = _participantMaxHp.asStateFlow()

    private val _tempParticipants = MutableStateFlow<List<EventParticipant>>(emptyList())
    val tempParticipants = _tempParticipants.asStateFlow()

    private val _editingParticipant = MutableStateFlow<EventParticipant?>(null)
    val editingParticipant = _editingParticipant.asStateFlow()

    private var loadSessionsJob: Job? = null

    fun loadCampaign(id: Int) {

        viewModelScope.launch {

            try {
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
                _campaign.value =
                    campaignRepository.getCampaignById(id)
                // Load existing participants
                _tempParticipants.value = _campaign.value?.participants ?: emptyList()
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campaign"
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
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
                sessionRepository
                    .getSessionsForCampaign(campaignId)
                    .collect { sessions ->

                        _sessionUiState.value =
                            _sessionUiState.value.copy(
                                sessions = sessions,
                                isLoading = false,
                                error = null
                            )
                    }
            } catch (e: Exception) {
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sessions"
                )
            }
        }
    }

    fun onSessionTitleChange(
        value: String
    ) {

        _sessionUiState.value =
            _sessionUiState.value.copy(
                sessionTitle = value
            )
    }

    fun onSessionNotesChange(
        value: String
    ) {

        _sessionUiState.value =
            _sessionUiState.value.copy(
                sessionNotes = value
            )
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
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
                sessionRepository.addSession(
                    campaignId = campaignId,
                    title = title,
                    notes = notes
                )

                _sessionUiState.value =
                    _sessionUiState.value.copy(
                        sessionTitle = "",
                        sessionNotes = "",
                        isLoading = false,
                        error = null
                    )
            } catch (e: Exception) {
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add session"
                )
            }
        }
    }

    fun deleteSession(
        id: Int
    ) {

        viewModelScope.launch {

            try {
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
                sessionRepository.deleteSession(id)
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _sessionUiState.value = _sessionUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete session"
                )
            }

        }
    }

    // Participant management methods
    fun onParticipantNameChange(value: String) {
        _participantName.value = value
    }

    fun onParticipantLevelChange(value: String) {
        _participantLevel.value = value
    }

    fun onParticipantMaxHpChange(value: String) {
        _participantMaxHp.value = value
    }

    fun addParticipant() {
        val name = _participantName.value
        val level = _participantLevel.value.toIntOrNull()
        val maxHp = _participantMaxHp.value.toIntOrNull()

        if (name.isBlank() || level == null || maxHp == null) return

        val participant = EventParticipant(
            name = name,
            level = level,
            maxHp = maxHp
        )

        _tempParticipants.value = _tempParticipants.value + participant
        _participantName.value = ""
        _participantLevel.value = ""
        _participantMaxHp.value = ""
    }

    fun removeParticipant(participant: EventParticipant) {
        _tempParticipants.value = _tempParticipants.value - participant
    }

    fun editParticipant(participant: EventParticipant) {
        _editingParticipant.value = participant
        _participantName.value = participant.name
        _participantLevel.value = participant.level.toString()
        _participantMaxHp.value = participant.maxHp.toString()
    }

    fun updateParticipant() {
        val editing = _editingParticipant.value ?: return
        val name = _participantName.value
        val level = _participantLevel.value.toIntOrNull()
        val maxHp = _participantMaxHp.value.toIntOrNull()

        if (name.isBlank() || level == null || maxHp == null) return

        val updatedParticipant = EventParticipant(
            name = name,
            level = level,
            maxHp = maxHp
        )

        // Replace in temp participants
        _tempParticipants.value = _tempParticipants.value.map {
            if (it == editing) updatedParticipant else it
        }

        // Clear edit state
        _editingParticipant.value = null
        _participantName.value = ""
        _participantLevel.value = ""
        _participantMaxHp.value = ""
    }

    fun cancelEdit() {
        _editingParticipant.value = null
        _participantName.value = ""
        _participantLevel.value = ""
        _participantMaxHp.value = ""
    }

    fun removeExistingParticipant(participant: EventParticipant) {
        val currentParticipants = _campaign.value?.participants ?: emptyList()
        val updatedParticipants = currentParticipants - participant
        _tempParticipants.value = updatedParticipants
    }

    fun saveParticipants(campaignId: Int) {
        viewModelScope.launch {
            try {
                campaignRepository.updateCampaignParticipants(
                    campaignId = campaignId,
                    participants = _tempParticipants.value
                )
                // Reload campaign to get updated data
                _campaign.value = campaignRepository.getCampaignById(campaignId)
            } catch (e: Exception) {
                _sessionUiState.value = _sessionUiState.value.copy(
                    error = e.message ?: "Failed to save participants"
                )
            }
        }
    }
}
