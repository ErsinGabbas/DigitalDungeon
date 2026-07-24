package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    val participantName by viewModel.participantName.collectAsState()
    val participantLevel by viewModel.participantLevel.collectAsState()
    val participantMaxHp by viewModel.participantMaxHp.collectAsState()
    val tempParticipants by viewModel.tempParticipants.collectAsState()
    val editingParticipant by viewModel.editingParticipant.collectAsState()

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

        // Show current participants
        val currentCampaign = campaign
        if (currentCampaign?.participants?.isNotEmpty() == true) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Campaign Participants:",
                    style = MaterialTheme.typography.titleSmall
                )
                currentCampaign.participants.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${participant.name} (Lvl ${participant.level}, HP: ${participant.maxHp})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.editParticipant(participant) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Edit")
                            }
                            Button(
                                onClick = { viewModel.removeExistingParticipant(participant) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        // Participant management section
        Text(
            text = "Manage Participants",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = participantName,
                onValueChange = viewModel::onParticipantNameChange,
                label = { Text("Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TextField(
                value = participantLevel,
                onValueChange = viewModel::onParticipantLevelChange,
                label = { Text("Lvl") },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            TextField(
                value = participantMaxHp,
                onValueChange = viewModel::onParticipantMaxHpChange,
                label = { Text("HP") },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (editingParticipant != null) {
                Button(
                    onClick = viewModel::updateParticipant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Update Participant")
                }
                Button(
                    onClick = viewModel::cancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = viewModel::addParticipant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Participant")
                }
                Button(
                    onClick = { viewModel.saveParticipants(campaignId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes")
                }
            }
        }

        // Show temp participants being added
        if (tempParticipants.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Participants to save:",
                    style = MaterialTheme.typography.labelSmall
                )
                tempParticipants.forEach { participant ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${participant.name} (Lvl ${participant.level}, HP: ${participant.maxHp})")
                        Button(
                            onClick = { viewModel.removeParticipant(participant) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
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
