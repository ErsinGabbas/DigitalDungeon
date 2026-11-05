package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignDetailViewModel
import com.example.digitaldungeon.ui.session.components.SessionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    campaignId: Int,
    navController: NavController,
    viewModel: CampaignDetailViewModel = hiltViewModel()
) {
    val state by viewModel.sessionUiState.collectAsStateWithLifecycle()
    val campaignState by viewModel.campaignUiState.collectAsStateWithLifecycle()
    val participantsState by viewModel.participantsUiState.collectAsStateWithLifecycle()

    val campaign = campaignState.campaign

    LaunchedEffect(campaignId) {

        viewModel.loadCampaign(
            campaignId
        )

        viewModel.loadSessions(
            campaignId
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(campaign?.name ?: "Campaign")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Session error display
            item {
                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                Text(
                    text = campaign?.name ?: "Campaign",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Campaign error display
            item {
                campaignState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item {
                campaign?.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
            }

            // Participants - single editable collection seeded from the campaign
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campaign Participants:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (participantsState.hasUnsavedChanges) {
                        Text(
                            text = "Unsaved changes",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Participant error display
            item {
                participantsState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            if (participantsState.participants.isEmpty()) {
                item {
                    Text(
                        text = "No participants yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            } else {
                itemsIndexed(participantsState.participants) { index, participant ->
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
                                onClick = { viewModel.editParticipant(index) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Edit")
                            }
                            Button(
                                onClick = { viewModel.removeParticipant(index) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            // Participant management section
            item {
                Text(
                    text = "Manage Participants",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = participantsState.participantName,
                        onValueChange = viewModel::onParticipantNameChange,
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextField(
                        value = participantsState.participantLevel,
                        onValueChange = viewModel::onParticipantLevelChange,
                        label = { Text("Lvl") },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                    TextField(
                        value = participantsState.participantMaxHp,
                        onValueChange = viewModel::onParticipantMaxHpChange,
                        label = { Text("HP") },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (participantsState.isEditing) {
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
                    }
                }
            }

            // Always available - editing a participant must never hide it
            item {
                Button(
                    onClick = { viewModel.saveParticipants(campaignId) },
                    enabled = !participantsState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    if (participantsState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }

            item {
                Text(
                    text = "Campaign ID: $campaignId",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
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
            }

            item {
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
            }

            item {
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
            }

            if (state.isLoading && state.sessions.isEmpty()) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

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
