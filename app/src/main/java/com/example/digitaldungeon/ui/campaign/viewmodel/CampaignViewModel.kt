package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val uiState: StateFlow<CampaignUiState> = _uiState.asStateFlow()

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

    fun onSearchQueryChange(
        value: String
    ) {

        _uiState.update {
            it.copy(
                searchQuery = value
            )
        }
    }

    fun clearSearchQuery() {

        _uiState.update {
            it.copy(searchQuery = "")
        }
    }

    fun addCampaign() {

        val name =
            _uiState.value.campaignName

        // Says why instead of silently doing nothing, the same way addSession does.
        if (name.isBlank()) {
            _uiState.update {
                it.copy(error = CAMPAIGN_NAME_REQUIRED)
            }
            return
        }

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

    /**
     * Asks for confirmation instead of deleting straight away: a campaign takes
     * all of its sessions and all of their events down with it.
     */
    fun requestDeleteCampaign(campaign: Campaign) {

        _uiState.update {
            // Only one dialog at a time: asking to delete closes the edit dialog.
            it.clearEditDialog().copy(
                pendingDeletion = PendingCampaignDeletion(
                    campaignId = campaign.id,
                    campaignName = campaign.name
                )
            )
        }
    }

    fun cancelDeleteCampaign() {

        _uiState.update {
            it.copy(pendingDeletion = null)
        }
    }

    fun confirmDeleteCampaign() {

        val pending =
            _uiState.value.pendingDeletion ?: return

        viewModelScope.launch {

            try {
                _uiState.update {
                    it.copy(
                        pendingDeletion = null,
                        isLoading = true
                    )
                }
                repository.deleteCampaign(pending.campaignId)
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

    /**
     * Opens the edit dialog on a copy of the stored name and description, so the
     * campaign in the list is only touched once the user saves.
     */
    fun requestEditCampaign(campaign: Campaign) {

        _uiState.update {
            it.copy(
                // Only one dialog at a time: editing closes a pending delete.
                pendingDeletion = null,
                editingCampaign = campaign,
                editName = campaign.name,
                editDescription = campaign.description,
                editError = null
            )
        }
    }

    fun onEditNameChange(
        value: String
    ) {

        _uiState.update {
            // Typing clears the rejection it caused.
            it.copy(
                editName = value,
                editError = null
            )
        }
    }

    fun onEditDescriptionChange(
        value: String
    ) {

        _uiState.update {
            it.copy(
                editDescription = value,
                editError = null
            )
        }
    }

    fun cancelEditCampaign() {

        _uiState.update { it.clearEditDialog() }
    }

    fun confirmEditCampaign() {

        val editing =
            _uiState.value.editingCampaign ?: return

        val name =
            _uiState.value.editName.trim()

        // Keeps the dialog open with the reason instead of closing it on a name
        // the campaign list could never show.
        if (name.isBlank()) {
            _uiState.update {
                it.copy(editError = CAMPAIGN_NAME_REQUIRED)
            }
            return
        }

        val description =
            _uiState.value.editDescription.trim()

        viewModelScope.launch {

            try {
                _uiState.update {
                    it.clearEditDialog().copy(isLoading = true)
                }
                repository.updateCampaign(
                    id = editing.id,
                    name = name,
                    description = description
                )
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
                        error = e.message ?: "Failed to update campaign"
                    )
                }
            }

        }

    }

    private fun CampaignUiState.clearEditDialog() =
        copy(
            editingCampaign = null,
            editName = "",
            editDescription = "",
            editError = null
        )

    companion object {

        const val CAMPAIGN_NAME_REQUIRED = "Campaign name cannot be empty"
    }
}
