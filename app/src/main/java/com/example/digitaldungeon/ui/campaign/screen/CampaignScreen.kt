package com.example.digitaldungeon.ui.campaign.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.campaign.components.BackupMenu
import com.example.digitaldungeon.ui.campaign.components.CampaignBackupActions
import com.example.digitaldungeon.ui.campaign.components.CampaignCard
import com.example.digitaldungeon.ui.campaign.components.CampaignListActions
import com.example.digitaldungeon.ui.campaign.components.CampaignSearchBar
import com.example.digitaldungeon.ui.campaign.components.ConfirmRestoreDialog
import com.example.digitaldungeon.ui.campaign.components.asBannerText
import com.example.digitaldungeon.ui.campaign.components.backupFileName
import com.example.digitaldungeon.ui.campaign.state.BackupMessage
import com.example.digitaldungeon.ui.campaign.state.BackupOperation
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignViewModel
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.components.EditDetailsDialog
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER
import com.example.digitaldungeon.ui.components.StatusBanner
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The campaign list with its ViewModel attached - the entry point the navigation
 * graph calls.
 *
 * Holds nothing but the three things that cannot be handed to a stateless
 * composable: the state subscription, the two system file pickers (they need an
 * activity result registry, which neither a preview nor `createComposeRule` can
 * provide) and the content resolver they write through. Everything the user can
 * see or press is decided by [CampaignScreenContent].
 */
@Composable
fun CampaignScreen(
    onOpenCampaign: (Int) -> Unit,
    viewModel: CampaignViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The only place in the backup flow that knows about Android. The ViewModel
    // deals in strings; where the string is read from and written to is decided
    // here, and the resolver comes from the application context so that an
    // operation outliving the activity cannot keep the activity alive with it.
    val contentResolver = LocalContext.current.applicationContext.contentResolver

    // The system save dialog. The user names the file and picks the folder, so no
    // storage permission is involved and the file lands somewhere they can find
    // it again - which is the entire point of a backup.
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->

        // null means the dialog was dismissed; nothing was promised, nothing to say.
        if (uri != null) {
            viewModel.exportBackup { json ->
                withContext(Dispatchers.IO) {
                    contentResolver.writeBackupText(uri, json)
                }
            }
        }
    }

    // Opened only after the warning has been accepted, so arriving here already
    // means the user knows the file they pick replaces everything.
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->

        if (uri != null) {
            viewModel.restoreBackup {
                withContext(Dispatchers.IO) {
                    contentResolver.readBackupText(uri)
                }
            }
        }
    }

    val actions = remember(viewModel) {
        CampaignListActions(
            onNameChange = viewModel::onCampaignNameChange,
            onDescriptionChange = viewModel::onCampaignDescriptionChange,
            onAddCampaign = viewModel::addCampaign,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onClearSearchQuery = viewModel::clearSearchQuery,
            onEditCampaignRequest = viewModel::requestEditCampaign,
            onEditNameChange = viewModel::onEditNameChange,
            onEditDescriptionChange = viewModel::onEditDescriptionChange,
            onConfirmEditCampaign = viewModel::confirmEditCampaign,
            onCancelEditCampaign = viewModel::cancelEditCampaign,
            onDeleteCampaignRequest = viewModel::requestDeleteCampaign,
            onConfirmDeleteCampaign = viewModel::confirmDeleteCampaign,
            onCancelDeleteCampaign = viewModel::cancelDeleteCampaign
        )
    }

    val backupActions = remember(viewModel, exportLauncher, restoreLauncher) {
        CampaignBackupActions(
            onExportBackup = {
                exportLauncher.launch(backupFileName())
            },
            onRestoreBackup = viewModel::requestRestoreBackup,
            onRestoreWarningConfirmed = {
                viewModel.dismissRestoreWarning()
                restoreLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
            },
            onRestoreWarningDismissed = viewModel::dismissRestoreWarning,
            onDismissMessage = viewModel::dismissBackupMessage
        )
    }

    CampaignScreenContent(
        state = state,
        onOpenCampaign = onOpenCampaign,
        actions = actions,
        backupActions = backupActions
    )
}

/**
 * The screen without its ViewModel, so it can be previewed and tested.
 *
 * Every pixel is a function of [state] and nothing else: there is no
 * `hiltViewModel()`, no `LocalContext` read beyond resource lookup, and no
 * remembered decision a caller cannot reproduce. Which dialog is open, which of
 * the two empty-list messages is shown and whether the backup menu accepts a
 * click are all questions [state] answers.
 *
 * Deliberately free of the file pickers: they live in [CampaignScreen] because
 * `rememberLauncherForActivityResult` needs an activity result registry, which a
 * preview has no way to provide. What reaches here instead is
 * [CampaignBackupActions] - five plain lambdas a test can count calls on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreenContent(
    state: CampaignUiState,
    onOpenCampaign: (Int) -> Unit,
    actions: CampaignListActions = CampaignListActions(),
    backupActions: CampaignBackupActions = CampaignBackupActions()
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.campaign_list_title))
                },
                actions = {
                    // Backup covers the whole database, not the campaign under a
                    // finger, so it belongs to the screen that stands for the
                    // app as a whole rather than to a campaign's own card.
                    BackupMenu(
                        onExportBackup = backupActions.onExportBackup,
                        onRestoreBackup = backupActions.onRestoreBackup,
                        enabled = state.isBackupMenuEnabled
                    )
                }
            )
        }
    ) { paddingValues ->

        val editingCampaign = state.editingCampaign
        val pendingDeletion = state.pendingDeletion

        // One branch, so editing, delete confirmation and the restore warning can
        // never be shown at once - the ViewModel also clears the others whenever
        // one is opened. Written as a `when` and not three `if`s on purpose: a
        // state that somehow carries two of them still shows exactly one dialog,
        // in this order of precedence.
        when {
            editingCampaign != null -> EditDetailsDialog(
                title = stringResource(R.string.campaign_edit_dialog_title),
                nameValue = state.editName,
                nameLabel = stringResource(R.string.campaign_name_label),
                detailsValue = state.editDescription,
                detailsLabel = stringResource(R.string.campaign_description_label),
                onNameChange = actions.onEditNameChange,
                onDetailsChange = actions.onEditDescriptionChange,
                onConfirm = actions.onConfirmEditCampaign,
                onDismiss = actions.onCancelEditCampaign,
                error = state.editError
            )

            pendingDeletion != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.campaign_delete_dialog_title),
                // The name goes in as an argument: a translation decides where
                // in the sentence it belongs.
                text = stringResource(
                    R.string.campaign_delete_dialog_text,
                    pendingDeletion.campaignName
                ),
                onConfirm = actions.onConfirmDeleteCampaign,
                onDismiss = actions.onCancelDeleteCampaign
            )

            state.isRestoreWarningVisible -> ConfirmRestoreDialog(
                onConfirm = backupActions.onRestoreWarningConfirmed,
                onDismiss = backupActions.onRestoreWarningDismissed
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Both of these sit directly under the app bar, above everything the
            // user came here to do: a restore that is still running, or one that
            // has just rewritten every campaign on screen, outranks the form.
            state.backupOperation?.let { operation ->
                BackupProgress(operation = operation)
            }

            state.backupMessage?.let { message ->
                StatusBanner(
                    text = message.asBannerText(),
                    onDismiss = backupActions.onDismissMessage,
                    isError = message.isError,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Why the last load, add, edit or delete failed.
            state.error?.let { error ->
                InlineErrorText(
                    error = error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            TextField(
                value = state.campaignName,
                onValueChange = actions.onNameChange,
                label = {
                    Text(stringResource(R.string.campaign_name_label))
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = state.campaignDescription,
                onValueChange = actions.onDescriptionChange,
                label = {
                    Text(stringResource(R.string.campaign_description_label))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = actions.onAddCampaign,
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.campaign_add_button))
                }
            }

            CampaignSearchBar(
                query = state.searchQuery,
                onQueryChange = actions.onSearchQueryChange,
                onClearQuery = actions.onClearSearchQuery,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (state.isLoading && state.campaigns.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            // "Nothing matched" is not "nothing created yet" - the two say
            // different things and only one of them is the user's own doing.
            if (state.hasNoCampaigns) {
                Text(
                    text = stringResource(R.string.campaign_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (state.hasNoSearchResults) {
                Text(
                    text = stringResource(
                        R.string.campaign_search_no_results,
                        state.searchQuery.trim()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(state.filteredCampaigns) { campaign ->

                    CampaignCard(
                        campaign = campaign,

                        onEdit = {
                            actions.onEditCampaignRequest(campaign)
                        },

                        onDelete = {
                            actions.onDeleteCampaignRequest(campaign)
                        },

                        onOpen = {
                            onOpenCampaign(campaign.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Says which of the two operations is running and that it has not finished yet.
 *
 * Indeterminate on purpose: neither the export nor the restore reports progress,
 * and a bar that filled up at a guessed rate would be inventing information.
 */
@Composable
private fun BackupProgress(
    operation: BackupOperation,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {

        Text(
            text = operation.progressLabel.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

private val previewCampaigns = listOf(
    Campaign(
        id = 1,
        name = "Curse of Strahd",
        description = "Gothic horror in Barovia",
        createdAt = PREVIEW_CREATED_AT_EARLIER,
        participants = listOf(
            EventParticipant(name = "Ireena", level = 4, maxHp = 28)
        )
    ),
    Campaign(
        id = 2,
        name = "Tomb of Annihilation",
        description = "Jungle death march",
        createdAt = PREVIEW_CREATED_AT
    )
)

/**
 * Renders [state] with every callback a no-op, so each preview below is one line
 * of state and nothing else - which is also exactly how a test will call the
 * screen when it only cares about what is drawn.
 */
@Composable
private fun PreviewCampaignScreen(state: CampaignUiState) {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = state,
            onOpenCampaign = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CampaignScreenContentPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            campaignName = "Lost Mine of Phandelver"
        )
    )
}

@Preview(name = "Search with results", showBackground = true)
@Composable
private fun CampaignScreenContentSearchingPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            searchQuery = "jungle"
        )
    )
}

@Preview(name = "Search without results", showBackground = true)
@Composable
private fun CampaignScreenContentNoSearchResultsPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            searchQuery = "dragonlance"
        )
    )
}

@Preview(name = "No campaigns yet", showBackground = true)
@Composable
private fun CampaignScreenContentEmptyPreview() {
    PreviewCampaignScreen(CampaignUiState())
}

@Preview(name = "First load", showBackground = true)
@Composable
private fun CampaignScreenContentLoadingPreview() {
    // Neither empty-list message may appear here: nothing is known yet, so
    // claiming there are no campaigns would be a guess.
    PreviewCampaignScreen(CampaignUiState(isLoading = true))
}

@Preview(name = "Campaign list failed", showBackground = true)
@Composable
private fun CampaignScreenContentErrorPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            error = UiText.of(R.string.campaign_error_load_failed)
        )
    )
}

@Preview(name = "Edit dialog open", showBackground = true)
@Composable
private fun CampaignScreenContentEditingPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            editingCampaign = previewCampaigns.first(),
            editName = "",
            editDescription = "Gothic horror in Barovia",
            editError = UiText.of(R.string.campaign_name_required)
        )
    )
}

@Preview(name = "Delete dialog open", showBackground = true)
@Composable
private fun CampaignScreenContentDeletingPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            pendingDeletion = PendingCampaignDeletion(
                campaignId = 1,
                campaignName = "Curse of Strahd"
            )
        )
    )
}

/**
 * Both dialog flags set at once, which the ViewModel never produces - the point
 * is that the screen still shows exactly one dialog if it ever did.
 */
@Preview(name = "Edit wins over delete", showBackground = true)
@Composable
private fun CampaignScreenContentTwoDialogsPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            editingCampaign = previewCampaigns.first(),
            editName = "Curse of Strahd",
            pendingDeletion = PendingCampaignDeletion(
                campaignId = 2,
                campaignName = "Tomb of Annihilation"
            ),
            isRestoreWarningVisible = true
        )
    )
}

@Preview(name = "Restore warning open", showBackground = true)
@Composable
private fun CampaignScreenContentRestoreWarningPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            isRestoreWarningVisible = true
        )
    )
}

@Preview(name = "Restore running", showBackground = true)
@Composable
private fun CampaignScreenContentRestoringPreview() {
    // Also the "backup menu refuses a second operation" case: the menu is
    // disabled for exactly as long as backupOperation is non-null.
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            backupOperation = BackupOperation.Restore
        )
    )
}

@Preview(name = "Restore succeeded", showBackground = true)
@Composable
private fun CampaignScreenContentRestoredPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            backupMessage = BackupMessage.RestoreSucceeded(
                totalRecords = 412,
                campaigns = 3,
                participants = 11,
                sessions = 18,
                events = 380
            )
        )
    )
}

@Preview(name = "Restore refused the file", showBackground = true)
@Composable
private fun CampaignScreenContentRestoreFailedPreview() {
    PreviewCampaignScreen(
        CampaignUiState(
            campaigns = previewCampaigns,
            backupMessage = BackupMessage.error(
                UiText.of(R.string.backup_restore_not_a_backup)
            )
        )
    )
}
