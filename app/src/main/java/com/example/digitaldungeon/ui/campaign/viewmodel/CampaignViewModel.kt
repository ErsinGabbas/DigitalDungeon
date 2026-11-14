package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asUiText
import com.example.digitaldungeon.ui.campaign.state.BackupMessage
import com.example.digitaldungeon.ui.campaign.state.BackupOperation
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.digitaldungeon.data.backup.BackupService
import com.example.digitaldungeon.data.backup.RestoreResult
import com.example.digitaldungeon.data.repository.CampaignRepository
import javax.inject.Inject

@HiltViewModel
class CampaignViewModel @Inject constructor(
    private val repository: CampaignRepository,
    private val backupService: BackupService
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
                        error = e.asUiText(R.string.campaign_error_load_failed)
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
                it.copy(error = UiText.of(R.string.campaign_name_required))
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
                        error = e.asUiText(R.string.campaign_error_add_failed)
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
            // Only one dialog at a time: asking to delete closes the edit dialog
            // and the restore warning.
            it.clearEditDialog().copy(
                isRestoreWarningVisible = false,
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
                        error = e.asUiText(R.string.campaign_error_delete_failed)
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
                // Only one dialog at a time: editing closes a pending delete and
                // the restore warning.
                pendingDeletion = null,
                isRestoreWarningVisible = false,
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
                it.copy(editError = UiText.of(R.string.campaign_name_required))
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
                        error = e.asUiText(R.string.campaign_error_update_failed)
                    )
                }
            }

        }

    }

    // ---------------------------------------------------------------------------
    // BACKUP AND RESTORE
    //
    // Whole-database operations, so they live on the campaign list rather than on
    // a single campaign. The ViewModel never sees a Context or a Uri: the screen
    // owns the file picker and hands over the two halves of the file access as
    // suspending lambdas, which also makes both flows testable without Android.
    // ---------------------------------------------------------------------------

    /**
     * Renders the database as a backup document and passes it to [writeJson],
     * which the caller is expected to implement off the main thread.
     *
     * The write is part of the operation, not a step after it: "backup saved" may
     * only be claimed once the bytes are in the file, so a failing [writeJson] is
     * reported as a failed export.
     *
     * @param writeJson stores the document; may throw, the failure is surfaced.
     */
    fun exportBackup(writeJson: suspend (String) -> Unit) {

        // One whole-database operation at a time. The menu is disabled too, but
        // the guard is what makes it true - a queued click must not get through.
        if (_uiState.value.isBackupRunning) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    backupOperation = BackupOperation.Export,
                    backupMessage = null
                )
            }

            try {

                val json = backupService.export()
                writeJson(json)

                val campaignCount = _uiState.value.campaigns.size

                _uiState.update {
                    it.copy(
                        backupOperation = null,
                        // The count travels as a count: how many plural forms it
                        // needs is the reader's language's business, not this
                        // method's.
                        backupMessage = BackupMessage.ExportSucceeded(campaignCount)
                    )
                }
            } catch (e: CancellationException) {
                // The screen is gone; reporting anything would be reporting to nobody.
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        backupOperation = null,
                        backupMessage = BackupMessage.error(
                            UiText.of(R.string.backup_export_failed, e.reason())
                        )
                    )
                }
            }

        }

    }

    /**
     * Opens the warning that has to be read before a restore can start.
     *
     * The file picker deliberately does *not* open here - it opens after the
     * warning is accepted, from the screen. A user who backs out of the picker
     * then simply changes nothing.
     */
    fun requestRestoreBackup() {

        _uiState.update {
            // Only one dialog at a time, same rule as edit and delete.
            it.clearEditDialog().copy(
                pendingDeletion = null,
                isRestoreWarningVisible = true,
                backupMessage = null
            )
        }
    }

    /**
     * Closes the restore warning.
     *
     * Used both by "Cancel" and by "Replace my data": accepting the warning only
     * takes it off the screen, after which the screen opens the file picker and
     * nothing is touched until a file actually comes back from it.
     */
    fun dismissRestoreWarning() {

        _uiState.update {
            it.copy(isRestoreWarningVisible = false)
        }
    }

    /**
     * Reads a backup document through [readJson] and replaces the database with
     * it. The read is expected to happen off the main thread.
     *
     * Every foreseeable failure ends up in [CampaignUiState.backupMessage] rather
     * than in a crash: an unreadable file and a file that is not a backup are both
     * ordinary outcomes of letting someone pick a file by hand.
     *
     * @param readJson returns the document's text; may throw, the failure is surfaced.
     */
    fun restoreBackup(readJson: suspend () -> String) {

        if (_uiState.value.isBackupRunning) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    backupOperation = BackupOperation.Restore,
                    isRestoreWarningVisible = false,
                    backupMessage = null
                )
            }

            val message =
                try {
                    backupService.restore(readJson()).toMessage()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    BackupMessage.error(
                        UiText.of(R.string.backup_restore_read_failed, e.reason())
                    )
                }

            _uiState.update {
                it.copy(
                    backupOperation = null,
                    backupMessage = message
                )
            }

        }

    }

    /** Takes the result banner off the screen; the user has read it. */
    fun dismissBackupMessage() {

        _uiState.update {
            it.copy(backupMessage = null)
        }
    }

    /**
     * Turns a restore outcome into something worth reading.
     *
     * No sentence is built here. The successful case is five numbers and the
     * failures are resource ids with their arguments - both are only turned into
     * text by the screen that draws them, which is the only place that knows
     * which language and which plural form that takes.
     */
    private fun RestoreResult.toMessage(): BackupMessage =
        when (this) {

            is RestoreResult.Success -> BackupMessage.RestoreSucceeded(
                totalRecords = totalRecords,
                campaigns = campaigns,
                participants = campaignParticipants,
                sessions = sessions,
                events = sessionEvents,
                quests = quests
            )

            RestoreResult.InvalidJson -> BackupMessage.error(
                UiText.of(R.string.backup_restore_not_a_backup)
            )

            is RestoreResult.UnsupportedFormatVersion -> BackupMessage.error(
                UiText.of(
                    R.string.backup_restore_unsupported_version,
                    formatVersion,
                    supportedFormatVersion
                )
            )
        }

    private fun CampaignUiState.clearEditDialog() =
        copy(
            editingCampaign = null,
            editName = "",
            editDescription = "",
            editError = null
        )

}

/**
 * Exceptions from file access are often messageless (an IOException from a
 * provider that handed back no stream, a SecurityException on a revoked
 * permission), and "Could not read the backup file: null" explains nothing.
 */
private fun Exception.reason(): String =
    message?.takeIf { it.isNotBlank() }
        ?: this::class.java.simpleName
