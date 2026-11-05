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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.navigation.NavController
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignViewModel
import com.example.digitaldungeon.ui.campaign.components.CampaignCard
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignScreen(
    navController: NavController,
    viewModel: CampaignViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Campaigns")
                }
            )
        }
    ) { paddingValues ->

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
                onValueChange = {
                    viewModel.onCampaignNameChange(it)
                },
                label = {
                    Text("Campaign Name")
                },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = state.campaignDescription,

                onValueChange = {
                    viewModel.onCampaignDescriptionChange(it)
                },

                label = {
                    Text("Campaign Description")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.addCampaign()
                },
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
                            viewModel.deleteCampaign(campaign.id)
                        },

                        onOpen = {
                            navController.navigate(
                                "details/${campaign.id}"
                            )
                        }
                    )
                }
            }
        }
    }
}
