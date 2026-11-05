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
import androidx.compose.foundation.lazy.items
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
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
        onDeleteCampaignRequest = viewModel::requestDeleteCampaign,
        onConfirmDeleteCampaign = viewModel::confirmDeleteCampaign,
        onCancelDeleteCampaign = viewModel::cancelDeleteCampaign
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
    onDeleteCampaignRequest: (Campaign) -> Unit,
    onConfirmDeleteCampaign: () -> Unit,
    onCancelDeleteCampaign: () -> Unit
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

        state.pendingDeletion?.let { pending ->
            ConfirmDeleteDialog(
                title = "Delete campaign?",
                text = "\"${pending.campaignName}\" will be deleted together with " +
                    "all of its sessions and every event recorded in them. " +
                    "This cannot be undone.",
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

            if (state.isLoading && state.campaigns.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(state.campaigns) { campaign ->

                    CampaignCard(
                        campaign = campaign,

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

@Preview(showBackground = true)
@Composable
private fun CampaignScreenContentPreview() {
    DigitalDungeonTheme {
        CampaignScreenContent(
            state = CampaignUiState(
                campaigns = listOf(
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
                ),
                campaignName = "Lost Mine of Phandelver"
            ),
            onCampaignNameChange = {},
            onCampaignDescriptionChange = {},
            onAddCampaign = {},
            onOpenCampaign = {},
            onDeleteCampaignRequest = {},
            onConfirmDeleteCampaign = {},
            onCancelDeleteCampaign = {}
        )
    }
}
