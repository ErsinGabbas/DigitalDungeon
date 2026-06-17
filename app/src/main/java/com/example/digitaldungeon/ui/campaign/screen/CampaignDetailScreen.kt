package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignDetailViewModel
import com.example.digitaldungeon.ui.session.components.SessionCard

@Composable
fun CampaignDetailScreen(
    campaignId: Int,
    navController: NavController,
    viewModel: CampaignDetailViewModel = hiltViewModel()
) {
    val state by viewModel.sessionUiState.collectAsState()
    val campaign by viewModel.campaign.collectAsState()

    LaunchedEffect(campaignId) {

        viewModel.loadCampaign(
            campaignId
        )

        viewModel.loadSessions(
            campaignId
        )
    }

    Column(
        modifier = Modifier
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

        Text(
            text = campaign?.name ?: "Campaign",
            style = MaterialTheme.typography.headlineMedium
        )

        campaign?.description
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        Text(
            text = "Campaign ID: $campaignId",
            style = MaterialTheme.typography.bodySmall
        )

        TextField(
            value = state.sessionTitle,
            onValueChange = {
                viewModel
                    .onSessionTitleChange(it)
            },
            label = {
                Text("Session Title")
            },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = state.sessionNotes,
            onValueChange = {
                viewModel
                    .onSessionNotesChange(it)
            },
            label = {
                Text("Session Notes")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                viewModel.addSession(
                    campaignId
                )
            },
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Add Session")
            }
        }

        if (state.isLoading && state.sessions.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        LazyColumn {
            items(state.sessions) { session ->
                SessionCard(
                    session = session,
                    onDelete = {
                        viewModel
                            .deleteSession(session.id)
                    },
                    onClick = {
                        navController.navigate("session/${session.id}")
                    }
                )
            }
        }
    }
}
