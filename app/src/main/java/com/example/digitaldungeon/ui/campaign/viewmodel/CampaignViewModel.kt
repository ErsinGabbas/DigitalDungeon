package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.getCampaigns()
                    .collect { campaigns ->

                        _uiState.value =
                            _uiState.value.copy(
                                campaigns = campaigns,
                                isLoading = false,
                                error = null
                            )

                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load campaigns"
                )
            }

        }

    }

    fun onCampaignNameChange(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                campaignName = value
            )

    }

    fun onCampaignDescriptionChange(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                campaignDescription = value
            )
    }

    fun addCampaign() {

        val name =
            _uiState.value.campaignName

        if (name.isBlank()) return

        viewModelScope.launch {

            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.addCampaign(
                    name = name,
                    description = _uiState.value.campaignDescription
                )

                _uiState.value =
                    _uiState.value.copy(
                        campaignName = "",
                        campaignDescription = "",
                        isLoading = false,
                        error = null
                    )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add campaign"
                )
            }

        }

    }

    fun deleteCampaign(id: Int) {

        viewModelScope.launch {

            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.deleteCampaign(id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete campaign"
                )
            }

        }

    }
}