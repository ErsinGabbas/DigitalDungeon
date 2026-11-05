package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TextField
import androidx.compose.foundation.lazy.LazyColumn
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.campaign.state.CampaignUiState
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignViewModel
import com.example.digitaldungeon.ui.campaign.components.CampaignCard
import com.example.digitaldungeon.ui.campaign.components.CampaignSearchBar
import androidx.compose.foundation.lazy.items
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.components.EditDetailsDialog
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

@Composable
fun CampaignScreen(
    onOpenCampaign: (Int) -> Unit,
    viewModel: CampaignViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CampaignScreenContent(
        state = state,
        onCampaignNameChange = viewModel::onCampaignNameChange,
        onCampaignDescriptionChange = viewModel::onCampaignDescriptionChange,
        onAddCampaign = viewModel::addCampaign,
        onOpenCampaign = onOpenCampaign,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onClearSearchQuery = viewModel::clearSearchQuery,
        onDeleteCampaignRequest = viewModel::requestDeleteCampaign,
        onConfirmDeleteCampaign = viewModel::confirmDeleteCampaign,
        onCancelDeleteCampaign = viewModel::cancelDeleteCampaign,
        onEditCampaignRequest = viewModel::requestEditCampaign,
        onEditNameChange = viewModel::onEditNameChange,
        onEditDescriptionChange = viewModel::onEditDescriptionChange,
        onConfirmEditCampaign = viewModel::confirmEditCampaign,
        onCancelEditCampaign = viewModel::cancelEditCampaign
    )
}

/**
 * The screen without its ViewModel, so it can be previewed and tested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreenContent(
    state: CampaignUiState,
    onCampaignNameChange: (String) -> Unit,
    onCampaignDescriptionChange: (String) -> Unit,
    onAddCampaign: () -> Unit,
    onOpenCampaign: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearchQuery: () -> Unit,
    onDeleteCampaignRequest: (Campaign) -> Unit,
    onConfirmDeleteCampaign: () -> Unit,
    onCancelDeleteCampaign: () -> Unit,
    onEditCampaignRequest: (Campaign) -> Unit,
    onEditNameChange: (String) -> Unit,
    onEditDescriptionChange: (String) -> Unit,
    onConfirmEditCampaign: () -> Unit,
    onCancelEditCampaign: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Campaigns")
                }
            )
        }
    ) { paddingValues ->

        val editingCampaign = state.editingCampaign
        val pendingDeletion = state.pendingDeletion

        // One branch, so editing and delete confirmation can never be shown at
        // once - the ViewModel also clears the other whenever one is opened.
        when {
            editingCampaign != null -> EditDetailsDialog(
                title = "Edit campaign",
                nameValue = state.editName,
                nameLabel = "Campaign Name",
                detailsValue = state.editDescription,
                detailsLabel = "Campaign Description",
                onNameChange = onEditNameChange,
                onDetailsChange = onEditDescriptionChange,
                onConfirm = onConfirmEditCampaign,
                onDismiss = onCancelEditCampaign,
                error = state.editError
            )

            pendingDeletion != null -> ConfirmDeleteDialog(
                title = "Delete campaign?",
                text = "\"${pendingDeletion.campaignName}\" will be deleted " +
                    "together with all of its sessions and every event recorded " +
                    "in them. This cannot be undone.",
                onConfirm = onConfirmDeleteCampaign,
                onDismiss = onCancelDeleteCampaign
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Error display
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            TextField(
                value = state.campaignName,
                onValueChange = onCampaignNameChange,
                label = {
                    Text("Campaign Name")
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = state.campaignDescription,
                onValueChange = onCampaignDescriptionChange,
                label = {
                    Text("Campaign Description")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onAddCampaign,
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Campaign")
                }
            }

            CampaignSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearQuery = onClearSearchQuery,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (state.isLoading && state.campaigns.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            // "Nothing matched" is not "nothing created yet" - the two say
            // different things and only one of them is the user's own doing.
            if (state.hasNoCampaigns) {
                Text(
                    text = "No campaigns yet",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else if (state.hasNoSearchResults) {
                Text(
                    text = "No campaigns match \"${state.searchQuery.trim()}\"",
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
                            onEditCampaignRequest(campaign)
                        },

                        onDelete = {
                            onDeleteCampaignRequest(campaign)
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

private val previewCampaigns = listOf(
    Campaign(
        id = 1,
        name = "Curse of Strahd",
        description = "Gothic horror in Barovia",
        createdAt = 0L,
        participants = listOf(
            EventParticipant(name = "Ireena", level = 4, maxHp = 28)
        )
    ),
    Campaign(
        id = 2,
        name = "Tomb of Annihilation",
        description = "Jungle death march",
        createdAt = 0L
    )
)

@Preview(showBackground = true)
@Composable
private fun CampaignScreenContentPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(
                campaigns = previewCampaigns,
                campaignName = "Lost Mine of Phandelver"
            ),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onSearchQueryChange = {},
            onClearSearchQuery = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {},
            onEditCampaignRequest = {},
            onEditNameChange = {},
            onEditDescriptionChange = {},
            onConfirmEditCampaign = {},
            onCancelEditCampaign = {}
        )
    }
}

@Preview(name = "Search with results", showBackground = true)
@Composable
private fun CampaignScreenContentSearchingPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(
                campaigns = previewCampaigns,
                searchQuery = "jungle"
            ),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onSearchQueryChange = {},
            onClearSearchQuery = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {},
            onEditCampaignRequest = {},
            onEditNameChange = {},
            onEditDescriptionChange = {},
            onConfirmEditCampaign = {},
            onCancelEditCampaign = {}
        )
    }
}

@Preview(name = "Search without results", showBackground = true)
@Composable
private fun CampaignScreenContentNoSearchResultsPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(
                campaigns = previewCampaigns,
                searchQuery = "dragonlance"
            ),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onSearchQueryChange = {},
            onClearSearchQuery = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {},
            onEditCampaignRequest = {},
            onEditNameChange = {},
            onEditDescriptionChange = {},
            onConfirmEditCampaign = {},
            onCancelEditCampaign = {}
        )
    }
}

@Preview(name = "No campaigns yet", showBackground = true)
@Composable
private fun CampaignScreenContentEmptyPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onSearchQueryChange = {},
            onClearSearchQuery = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {},
            onEditCampaignRequest = {},
            onEditNameChange = {},
            onEditDescriptionChange = {},
            onConfirmEditCampaign = {},
            onCancelEditCampaign = {}
        )
    }
}

@Preview(name = "Edit dialog open", showBackground = true)
@Composable
private fun CampaignScreenContentEditingPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(
                campaigns = previewCampaigns,
                editingCampaign = previewCampaigns.first(),
                editName = "",
                editDescription = "Gothic horror in Barovia",
                editError = "Campaign name cannot be empty"
            ),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onSearchQueryChange = {},
            onClearSearchQuery = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {},
            onEditCampaignRequest = {},
            onEditNameChange = {},
            onEditDescriptionChange = {},
            onConfirmEditCampaign = {},
            onCancelEditCampaign = {}
        )
    }
}
