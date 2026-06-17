package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.domain.model.Campaign
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

    private var loadSessionsJob: Job? = null

    fun loadCampaign(id: Int) {

        viewModelScope.launch {

            try {
                _sessionUiState.value = _sessionUiState.value.copy(isLoading = true)
                _campaign.value =
                    campaignRepository.getCampaignById(id)
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
}
