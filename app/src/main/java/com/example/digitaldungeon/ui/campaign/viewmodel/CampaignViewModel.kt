package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.digitaldungeon.data.repository.CampaignRepository
import javax.inject.Inject

@HiltViewModel
class CampaignViewModel @Inject constructor(
    private val repository: CampaignRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(CampaignUiState())

    val uiState = _uiState.asStateFlow()

    init {

        viewModelScope.launch {

            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.getCampaigns()
                    .collect { campaigns ->

                        _uiState.update {
                            it.copy(
                                campaigns = campaigns,
                                isLoading = false,
                                error = null
                            )
                        }

                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load campaigns"
                    )
                }
            }

        }

    }

    fun onCampaignNameChange(
        value: String
    ) {

        _uiState.update {
            it.copy(
                campaignName = value
            )
        }

    }

    fun onCampaignDescriptionChange(
        value: String
    ) {

        _uiState.update {
            it.copy(
                campaignDescription = value
            )
        }
    }

    fun addCampaign() {

        val name =
            _uiState.value.campaignName

        if (name.isBlank()) return

        viewModelScope.launch {

            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.addCampaign(
                    name = name,
                    description = _uiState.value.campaignDescription
                )

                _uiState.update {
                    it.copy(
                        campaignName = "",
                        campaignDescription = "",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to add campaign"
                    )
                }
            }

        }

    }

    fun deleteCampaign(id: Int) {

        viewModelScope.launch {

            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.deleteCampaign(id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete campaign"
                    )
                }
            }

        }

    }
}
