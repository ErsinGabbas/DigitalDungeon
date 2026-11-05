package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitaldungeon.ui.campaign.components.ParticipantForm
import com.example.digitaldungeon.ui.campaign.components.ParticipantRow
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignDetailViewModel
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.session.components.SessionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    onBack: () -> Unit,
    onOpenSession: (Int) -> Unit,
    viewModel: CampaignDetailViewModel = hiltViewModel()
) {
    val state by viewModel.sessionUiState.collectAsStateWithLifecycle()
    val campaignState by viewModel.campaignUiState.collectAsStateWithLifecycle()
    val participantsState by viewModel.participantsUiState.collectAsStateWithLifecycle()

    val campaign = campaignState.campaign

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(campaign?.name ?: "Campaign")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
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

        state.pendingDeletion?.let { pending ->
            ConfirmDeleteDialog(
                title = "Delete session?",
                text = "\"${pending.sessionTitle}\" will be deleted together with " +
                    "every event recorded in it. This cannot be undone.",
                onConfirm = viewModel::confirmDeleteSession,
                onDismiss = viewModel::cancelDeleteSession
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Session error display. The null check stays outside item {}: an item that
            // renders nothing is still an item, and the list's spacing leaves a blank gap.
            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // The campaign name is already the TopAppBar title - no second heading here.

            // Campaign error display
            campaignState.error?.let { error ->
                item {
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
            participantsState.error?.let { error ->
                item {
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
                    ParticipantRow(
                        participant = participant,
                        onEdit = { viewModel.editParticipant(index) },
                        onRemove = { viewModel.removeParticipant(index) }
                    )
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
                ParticipantForm(
                    name = participantsState.participantName,
                    level = participantsState.participantLevel,
                    maxHp = participantsState.participantMaxHp,
                    isEditing = participantsState.isEditing,
                    onNameChange = viewModel::onParticipantNameChange,
                    onLevelChange = viewModel::onParticipantLevelChange,
                    onMaxHpChange = viewModel::onParticipantMaxHpChange,
                    onAdd = viewModel::addParticipant,
                    onUpdate = viewModel::updateParticipant,
                    onCancelEdit = viewModel::cancelEdit
                )
            }

            // Always available - editing a participant must never hide it
            item {
                Button(
                    onClick = viewModel::saveParticipants,
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
                TextField(
                    value = state.sessionTitle,
                    onValueChange = viewModel::onSessionTitleChange,
                    label = {
                        Text("Session Title")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                TextField(
                    value = state.sessionNotes,
                    onValueChange = viewModel::onSessionNotesChange,
                    label = {
                        Text("Session Notes")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = viewModel::addSession,
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
                        viewModel.requestDeleteSession(session)
                    },
                    onClick = {
                        onOpenSession(session.id)
                    }
                )
            }
        }
    }
}
