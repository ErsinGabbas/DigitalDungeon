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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.ui.campaign.components.CampaignParticipantActions
import com.example.digitaldungeon.ui.campaign.components.CampaignSessionActions
import com.example.digitaldungeon.ui.campaign.components.ParticipantForm
import com.example.digitaldungeon.ui.campaign.components.ParticipantRow
import com.example.digitaldungeon.ui.campaign.components.SessionListItem
import com.example.digitaldungeon.ui.campaign.state.CampaignDetailUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignParticipantsUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignSessionsUiState
import com.example.digitaldungeon.ui.campaign.state.PendingSessionDeletion
import com.example.digitaldungeon.ui.campaign.viewmodel.CampaignDetailViewModel
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.components.EditDetailsDialog
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One campaign with its ViewModel attached - the entry point the navigation graph
 * calls.
 *
 * A thin wrapper by design: it subscribes to the three state flows the screen is
 * built from and binds the twenty callbacks to the ViewModel. It decides nothing
 * and draws nothing; everything visible is [CampaignDetailScreenContent].
 *
 * Three flows and not one, because the campaign, its party and its sessions fail
 * independently - see [CampaignDetailUiState], [CampaignParticipantsUiState] and
 * [CampaignSessionsUiState]. Collecting them separately is also what keeps a
 * keystroke in the participant form from recomposing the session list.
 */
@Composable
fun CampaignDetailScreen(
    onBack: () -> Unit,
    onOpenSession: (Int) -> Unit,
    viewModel: CampaignDetailViewModel = hiltViewModel()
) {
    val campaignState by viewModel.campaignUiState.collectAsStateWithLifecycle()
    val participantsState by viewModel.participantsUiState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionUiState.collectAsStateWithLifecycle()

    val participantActions = remember(viewModel) {
        CampaignParticipantActions(
            onNameChange = viewModel::onParticipantNameChange,
            onLevelChange = viewModel::onParticipantLevelChange,
            onMaxHpChange = viewModel::onParticipantMaxHpChange,
            onAdd = viewModel::addParticipant,
            onUpdate = viewModel::updateParticipant,
            onCancelEdit = viewModel::cancelEdit,
            onEditParticipant = viewModel::editParticipant,
            onRemoveParticipant = viewModel::removeParticipant,
            onSave = viewModel::saveParticipants
        )
    }

    val sessionActions = remember(viewModel) {
        CampaignSessionActions(
            onTitleChange = viewModel::onSessionTitleChange,
            onNotesChange = viewModel::onSessionNotesChange,
            onAddSession = viewModel::addSession,
            onEditSessionRequest = viewModel::requestEditSession,
            onEditTitleChange = viewModel::onEditSessionTitleChange,
            onEditNotesChange = viewModel::onEditSessionNotesChange,
            onConfirmEditSession = viewModel::confirmEditSession,
            onCancelEditSession = viewModel::cancelEditSession,
            onDeleteSessionRequest = viewModel::requestDeleteSession,
            onConfirmDeleteSession = viewModel::confirmDeleteSession,
            onCancelDeleteSession = viewModel::cancelDeleteSession
        )
    }

    CampaignDetailScreenContent(
        campaignState = campaignState,
        participantsState = participantsState,
        sessionsState = sessionsState,
        onBack = onBack,
        onOpenSession = onOpenSession,
        participantActions = participantActions,
        sessionActions = sessionActions
    )
}

/**
 * The campaign detail screen without its ViewModel, so it can be previewed and
 * tested.
 *
 * Everything on screen is a function of the three state arguments: the top bar
 * title, which of the two dialogs is open, which of the three error channels has
 * something to say, whether the party has unsaved edits and whether the
 * participant list is empty. No `hiltViewModel()`, no hidden `remember` that a
 * caller cannot reproduce.
 *
 * One [LazyColumn] for the whole screen - the participant rows and the session
 * list are items of it rather than lists of their own. Nested lazy lists in the
 * same direction cannot be measured, and wrapping this in a `verticalScroll`
 * would do the same thing by another route; both would also throw away the one
 * reason this is lazy, which is a campaign with two hundred sessions.
 *
 * @param onBack the navigate-up button. Navigation, so it comes from the graph.
 * @param onOpenSession a session row was opened, by id. Navigation as well, which
 * is why neither of these two is part of [CampaignSessionActions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreenContent(
    campaignState: CampaignDetailUiState,
    participantsState: CampaignParticipantsUiState,
    sessionsState: CampaignSessionsUiState,
    onBack: () -> Unit,
    onOpenSession: (Int) -> Unit,
    participantActions: CampaignParticipantActions = CampaignParticipantActions(),
    sessionActions: CampaignSessionActions = CampaignSessionActions()
) {

    val campaign = campaignState.campaign

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        campaign?.name
                            ?: stringResource(R.string.campaign_detail_title_fallback)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        val editingSession = sessionsState.editingSession
        val pendingDeletion = sessionsState.pendingDeletion

        // One branch, so editing and delete confirmation can never be shown at
        // once - the ViewModel also clears the other whenever one is opened.
        // Written as a `when` and not two `if`s on purpose: a state that somehow
        // carries both still shows exactly one dialog, and this is the order of
        // precedence it shows them in.
        when {
            editingSession != null -> EditDetailsDialog(
                title = stringResource(R.string.campaign_session_edit_dialog_title),
                nameValue = sessionsState.editSessionTitle,
                nameLabel = stringResource(R.string.campaign_session_title_label),
                detailsValue = sessionsState.editSessionNotes,
                detailsLabel = stringResource(R.string.campaign_session_notes_label),
                onNameChange = sessionActions.onEditTitleChange,
                onDetailsChange = sessionActions.onEditNotesChange,
                onConfirm = sessionActions.onConfirmEditSession,
                onDismiss = sessionActions.onCancelEditSession,
                error = sessionsState.editError
            )

            pendingDeletion != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.campaign_session_delete_dialog_title),
                text = stringResource(
                    R.string.campaign_session_delete_dialog_text,
                    pendingDeletion.sessionTitle
                ),
                onConfirm = sessionActions.onConfirmDeleteSession,
                onDismiss = sessionActions.onCancelDeleteSession
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Three error channels, each fed by its own state object and each
            // with its own fixed place in this list, so a failed save of the
            // party cannot wipe out "campaign not found" or a session that would
            // not delete - all three can be on screen at the same time.
            //
            // The two that concern the whole screen are kept at the top where
            // they cannot be scrolled past; the participant one sits down with
            // the participants, because that is the only thing it is about.
            //
            // The null checks stay outside item {}: an item that renders nothing
            // is still an item, and the list's spacing leaves a blank gap for it.

            // Sessions: load / add / update / delete.
            sessionsState.error?.let { error ->
                item {
                    InlineErrorText(
                        error = error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // The campaign name is already the TopAppBar title - no second heading here.

            // The campaign itself: it would not load, or there is no such campaign.
            campaignState.error?.let { error ->
                item {
                    InlineErrorText(
                        error = error,
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
                        text = stringResource(R.string.campaign_participants_heading),
                        style = MaterialTheme.typography.titleSmall
                    )

                    // The party is edited in memory and saved as a whole, so this
                    // is the only thing standing between a rearranged party and a
                    // navigate-up that drops it.
                    if (participantsState.hasUnsavedChanges) {
                        Text(
                            text = stringResource(R.string.campaign_participants_unsaved),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // The party: a refused form, or a save that failed.
            participantsState.error?.let { error ->
                item {
                    InlineErrorText(
                        error = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            if (participantsState.participants.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.campaign_participants_empty),
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
                        onEdit = { participantActions.onEditParticipant(index) },
                        onRemove = { participantActions.onRemoveParticipant(index) }
                    )
                }
            }

            // Participant management section
            item {
                Text(
                    text = stringResource(R.string.campaign_participants_manage),
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
                    onNameChange = participantActions.onNameChange,
                    onLevelChange = participantActions.onLevelChange,
                    onMaxHpChange = participantActions.onMaxHpChange,
                    onAdd = participantActions.onAdd,
                    onUpdate = participantActions.onUpdate,
                    onCancelEdit = participantActions.onCancelEdit
                )
            }

            // Always available - editing a participant must never hide it
            item {
                Button(
                    onClick = participantActions.onSave,
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
                        Text(stringResource(R.string.campaign_participants_save))
                    }
                }
            }

            item {
                TextField(
                    value = sessionsState.sessionTitle,
                    onValueChange = sessionActions.onTitleChange,
                    label = {
                        Text(stringResource(R.string.campaign_session_title_label))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                TextField(
                    value = sessionsState.sessionNotes,
                    onValueChange = sessionActions.onNotesChange,
                    label = {
                        Text(stringResource(R.string.campaign_session_notes_label))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = sessionActions.onAddSession,
                    enabled = !sessionsState.isLoading
                ) {
                    if (sessionsState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.campaign_session_add_button))
                    }
                }
            }

            if (sessionsState.isLoading && sessionsState.sessions.isEmpty()) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            items(sessionsState.sessions) { session ->
                SessionListItem(
                    session = session,
                    onOpen = {
                        onOpenSession(session.id)
                    },
                    onEdit = {
                        sessionActions.onEditSessionRequest(session)
                    },
                    onDelete = {
                        sessionActions.onDeleteSessionRequest(session)
                    }
                )
            }
        }
    }
}

private val previewParty = listOf(
    EventParticipant(name = "Ireena", level = 4, maxHp = 28),
    EventParticipant(name = "Ismark", level = 3, maxHp = 24)
)

private val previewCampaign = Campaign(
    id = 1,
    name = "Curse of Strahd",
    description = "Gothic horror in the valley of Barovia",
    createdAt = PREVIEW_CREATED_AT_EARLIER,
    participants = previewParty
)

private val previewSessions = listOf(
    Session(
        id = 11,
        campaignId = 1,
        title = "Session 3 - Death House",
        notes = "The nursery, and what the party found under it.",
        createdAt = PREVIEW_CREATED_AT_EARLIER
    ),
    Session(
        id = 12,
        campaignId = 1,
        title = "Session 4 - Barovia by day",
        notes = "Ismark asks for an escort to Vallaki.",
        createdAt = PREVIEW_CREATED_AT
    )
)

/**
 * Renders the three states with every callback a no-op, so each preview below is
 * state and nothing else - which is also how a test calls the screen when it only
 * cares about what is drawn.
 */
@Composable
private fun PreviewCampaignDetailScreen(
    campaignState: CampaignDetailUiState = CampaignDetailUiState(
        campaignId = 1,
        campaign = previewCampaign
    ),
    participantsState: CampaignParticipantsUiState = CampaignParticipantsUiState(
        participants = previewParty,
        savedParticipants = previewParty
    ),
    sessionsState: CampaignSessionsUiState = CampaignSessionsUiState(
        sessions = previewSessions
    )
) {
    DigitalDungeonTheme {
        CampaignDetailScreenContent(
            campaignState = campaignState,
            participantsState = participantsState,
            sessionsState = sessionsState,
            onBack = {},
            onOpenSession = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CampaignDetailScreenContentPreview() {
    PreviewCampaignDetailScreen()
}

@Preview(name = "No participants yet", showBackground = true)
@Composable
private fun CampaignDetailScreenContentNoParticipantsPreview() {
    PreviewCampaignDetailScreen(
        campaignState = CampaignDetailUiState(
            campaignId = 1,
            campaign = previewCampaign.copy(participants = emptyList())
        ),
        participantsState = CampaignParticipantsUiState()
    )
}

@Preview(name = "No sessions yet", showBackground = true)
@Composable
private fun CampaignDetailScreenContentNoSessionsPreview() {
    PreviewCampaignDetailScreen(
        sessionsState = CampaignSessionsUiState()
    )
}

/**
 * A party edited but not saved. The warning next to the heading is the only sign
 * the user gets, so it is worth being able to look at.
 */
@Preview(name = "Unsaved party changes", showBackground = true)
@Composable
private fun CampaignDetailScreenContentUnsavedPreview() {
    PreviewCampaignDetailScreen(
        participantsState = CampaignParticipantsUiState(
            participants = previewParty.dropLast(1),
            savedParticipants = previewParty
        )
    )
}

@Preview(name = "Editing a participant", showBackground = true)
@Composable
private fun CampaignDetailScreenContentEditingParticipantPreview() {
    PreviewCampaignDetailScreen(
        participantsState = CampaignParticipantsUiState(
            participants = previewParty,
            savedParticipants = previewParty,
            participantName = "Ismark",
            participantLevel = "3",
            participantMaxHp = "0",
            editingIndex = 1,
            error = UiText.of(
                R.string.campaign_participant_max_hp_out_of_range,
                CampaignDetailViewModel.MIN_MAX_HP,
                CampaignDetailViewModel.MAX_MAX_HP
            )
        )
    )
}

@Preview(name = "Saving the party", showBackground = true)
@Composable
private fun CampaignDetailScreenContentSavingPreview() {
    PreviewCampaignDetailScreen(
        participantsState = CampaignParticipantsUiState(
            participants = previewParty,
            savedParticipants = previewParty.dropLast(1),
            isSaving = true
        )
    )
}

@Preview(name = "Session edit dialog open", showBackground = true)
@Composable
private fun CampaignDetailScreenContentEditSessionPreview() {
    PreviewCampaignDetailScreen(
        sessionsState = CampaignSessionsUiState(
            sessions = previewSessions,
            editingSession = previewSessions.first(),
            editSessionTitle = "",
            editSessionNotes = "The nursery, and what the party found under it.",
            editError = UiText.of(R.string.campaign_session_title_required)
        )
    )
}

@Preview(name = "Session delete dialog open", showBackground = true)
@Composable
private fun CampaignDetailScreenContentDeleteSessionPreview() {
    PreviewCampaignDetailScreen(
        sessionsState = CampaignSessionsUiState(
            sessions = previewSessions,
            pendingDeletion = PendingSessionDeletion(
                sessionId = 11,
                sessionTitle = "Session 3 - Death House"
            )
        )
    )
}

/**
 * Both dialog fields set at once, which the ViewModel never produces - the point
 * is that the screen still shows exactly one dialog if it ever did.
 */
@Preview(name = "Edit wins over delete", showBackground = true)
@Composable
private fun CampaignDetailScreenContentTwoDialogsPreview() {
    PreviewCampaignDetailScreen(
        sessionsState = CampaignSessionsUiState(
            sessions = previewSessions,
            editingSession = previewSessions.first(),
            editSessionTitle = "Session 3 - Death House",
            pendingDeletion = PendingSessionDeletion(
                sessionId = 12,
                sessionTitle = "Session 4 - Barovia by day"
            )
        )
    )
}

/**
 * All three error channels at once - the state the split into three flows exists
 * for. Each message must survive the other two.
 */
@Preview(name = "Three errors at once", showBackground = true)
@Composable
private fun CampaignDetailScreenContentAllErrorsPreview() {
    PreviewCampaignDetailScreen(
        campaignState = CampaignDetailUiState(
            campaignId = 1,
            campaign = previewCampaign,
            error = UiText.of(R.string.campaign_error_load_one_failed)
        ),
        participantsState = CampaignParticipantsUiState(
            participants = previewParty,
            savedParticipants = previewParty,
            error = UiText.of(R.string.campaign_error_participants_save_failed)
        ),
        sessionsState = CampaignSessionsUiState(
            sessions = previewSessions,
            error = UiText.of(R.string.campaign_error_session_delete_failed)
        )
    )
}

/**
 * Opened on a campaign that is gone: the title falls back, the error says so and
 * the party is empty rather than stale.
 */
@Preview(name = "Campaign not found", showBackground = true)
@Composable
private fun CampaignDetailScreenContentNotFoundPreview() {
    PreviewCampaignDetailScreen(
        campaignState = CampaignDetailUiState(
            campaignId = 404,
            error = UiText.of(R.string.campaign_not_found)
        ),
        participantsState = CampaignParticipantsUiState(),
        sessionsState = CampaignSessionsUiState()
    )
}

@Preview(name = "First load", showBackground = true)
@Composable
private fun CampaignDetailScreenContentLoadingPreview() {
    PreviewCampaignDetailScreen(
        campaignState = CampaignDetailUiState(campaignId = 1, isLoading = true),
        participantsState = CampaignParticipantsUiState(),
        sessionsState = CampaignSessionsUiState(isLoading = true)
    )
}
