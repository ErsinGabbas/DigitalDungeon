package com.example.digitaldungeon.ui.quest.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.components.CreatedAtLabel
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER
import com.example.digitaldungeon.ui.quest.components.PREVIEW_QUEST
import com.example.digitaldungeon.ui.quest.components.PREVIEW_QUEST_HIDDEN
import com.example.digitaldungeon.ui.quest.components.PREVIEW_QUEST_MINIMAL
import com.example.digitaldungeon.ui.quest.components.QuestConsequenceActions
import com.example.digitaldungeon.ui.quest.components.QuestDetailActions
import com.example.digitaldungeon.ui.quest.components.QuestNoteActions
import com.example.digitaldungeon.ui.quest.components.QuestObjectiveActions
import com.example.digitaldungeon.ui.quest.components.QuestRelationActions
import com.example.digitaldungeon.ui.quest.components.QuestSessionActions
import com.example.digitaldungeon.ui.quest.components.QuestRewardActions
import com.example.digitaldungeon.ui.quest.components.QuestStatusSelector
import com.example.digitaldungeon.ui.quest.components.consequencesSection
import com.example.digitaldungeon.ui.quest.components.historySection
import com.example.digitaldungeon.ui.quest.components.label
import com.example.digitaldungeon.ui.quest.components.notesSection
import com.example.digitaldungeon.ui.quest.components.objectivesSection
import com.example.digitaldungeon.ui.quest.components.questSessionsSection
import com.example.digitaldungeon.ui.quest.components.relationsSection
import com.example.digitaldungeon.ui.quest.components.rewardsSection
import com.example.digitaldungeon.ui.quest.state.PendingObjectiveDeletion
import com.example.digitaldungeon.ui.quest.state.QuestConsequencesUiState
import com.example.digitaldungeon.ui.quest.state.QuestDetailUiState
import com.example.digitaldungeon.ui.quest.state.QuestNotesUiState
import com.example.digitaldungeon.ui.quest.state.QuestObjectivesUiState
import com.example.digitaldungeon.ui.quest.state.QuestHistoryUiState
import com.example.digitaldungeon.ui.quest.state.QuestRelationsUiState
import com.example.digitaldungeon.ui.quest.state.QuestSessionsUiState
import com.example.digitaldungeon.ui.quest.state.QuestRewardsUiState
import com.example.digitaldungeon.ui.quest.viewmodel.QuestDetailViewModel
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One quest with its ViewModel attached - the entry point the navigation graph calls.
 *
 * The quest id is not a parameter: it reaches the ViewModel through `SavedStateHandle`, the same
 * way the session screen gets its session.
 *
 * Six state flows are collected here, one per block, because the quest and the five things
 * hanging off it fail independently - see [QuestDetailViewModel].
 *
 * @param onBack the navigate-up button.
 * @param onEditQuest opens the form on this quest, by id.
 * @param onOpenQuest opens a linked quest, by id - which is how a story arc is walked through
 * without going back to the list.
 * @param onOpenSession opens a session this quest came up in, by id. All four are navigation, so
 * they come from the graph.
 */
@Composable
fun QuestDetailScreen(
    onBack: () -> Unit,
    onEditQuest: (Int) -> Unit,
    onOpenQuest: (Int) -> Unit,
    onOpenSession: (Int) -> Unit,
    viewModel: QuestDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val objectivesState by viewModel.objectivesUiState.collectAsStateWithLifecycle()
    val rewardsState by viewModel.rewardsUiState.collectAsStateWithLifecycle()
    val consequencesState by viewModel.consequencesUiState.collectAsStateWithLifecycle()
    val notesState by viewModel.notesUiState.collectAsStateWithLifecycle()
    val relationsState by viewModel.relationsUiState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyUiState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsUiState.collectAsStateWithLifecycle()

    val actions = remember(viewModel) {
        QuestDetailActions(
            onStatusSelected = viewModel::onStatusSelected
        )
    }

    val objectiveActions = remember(viewModel) {
        QuestObjectiveActions(
            onTitleChange = viewModel::onObjectiveTitleChange,
            onDescriptionChange = viewModel::onObjectiveDescriptionChange,
            onOptionalChange = viewModel::onObjectiveOptionalChange,
            onAdd = viewModel::onAddObjective,
            onUpdate = viewModel::onUpdateObjective,
            onEditRequest = viewModel::onEditObjectiveRequest,
            onCancelEdit = viewModel::onCancelObjectiveEdit,
            onStatusSelected = viewModel::onObjectiveStatusSelected,
            onMoveUp = viewModel::onMoveObjectiveUp,
            onMoveDown = viewModel::onMoveObjectiveDown,
            onDeleteRequest = viewModel::onObjectiveDeleteRequest,
            onConfirmDelete = viewModel::onConfirmObjectiveDelete,
            onCancelDelete = viewModel::onCancelObjectiveDelete
        )
    }

    val rewardActions = remember(viewModel) {
        QuestRewardActions(
            onTitleChange = viewModel::onRewardTitleChange,
            onDescriptionChange = viewModel::onRewardDescriptionChange,
            onTypeSelected = viewModel::onRewardTypeSelected,
            onAdd = viewModel::onAddReward,
            onUpdate = viewModel::onUpdateReward,
            onEditRequest = viewModel::onEditRewardRequest,
            onCancelEdit = viewModel::onCancelRewardEdit,
            onDeleteRequest = viewModel::onRewardDeleteRequest,
            onConfirmDelete = viewModel::onConfirmRewardDelete,
            onCancelDelete = viewModel::onCancelRewardDelete
        )
    }

    val consequenceActions = remember(viewModel) {
        QuestConsequenceActions(
            onTitleChange = viewModel::onConsequenceTitleChange,
            onDescriptionChange = viewModel::onConsequenceDescriptionChange,
            onTypeSelected = viewModel::onConsequenceTypeSelected,
            onAdd = viewModel::onAddConsequence,
            onUpdate = viewModel::onUpdateConsequence,
            onEditRequest = viewModel::onEditConsequenceRequest,
            onCancelEdit = viewModel::onCancelConsequenceEdit,
            onDeleteRequest = viewModel::onConsequenceDeleteRequest,
            onConfirmDelete = viewModel::onConfirmConsequenceDelete,
            onCancelDelete = viewModel::onCancelConsequenceDelete
        )
    }

    val noteActions = remember(viewModel) {
        QuestNoteActions(
            onContentChange = viewModel::onNoteContentChange,
            onAdd = viewModel::onAddNote,
            onUpdate = viewModel::onUpdateNote,
            onEditRequest = viewModel::onEditNoteRequest,
            onCancelEdit = viewModel::onCancelNoteEdit,
            onDeleteRequest = viewModel::onNoteDeleteRequest,
            onConfirmDelete = viewModel::onConfirmNoteDelete,
            onCancelDelete = viewModel::onCancelNoteDelete
        )
    }

    val relationActions = remember(viewModel) {
        QuestRelationActions(
            onKindSelected = viewModel::onRelationKindSelected,
            onTargetSelected = viewModel::onRelationTargetSelected,
            onAdd = viewModel::onAddRelation,
            onDeleteRequest = viewModel::onRelationDeleteRequest,
            onConfirmDelete = viewModel::onConfirmRelationDelete,
            onCancelDelete = viewModel::onCancelRelationDelete
        )
    }

    val sessionActions = remember(viewModel) {
        QuestSessionActions(
            onSessionSelected = viewModel::onSessionSelected,
            onAdd = viewModel::onAddSessionLink,
            onDeleteRequest = viewModel::onSessionLinkDeleteRequest,
            onConfirmDelete = viewModel::onConfirmSessionLinkDelete,
            onCancelDelete = viewModel::onCancelSessionLinkDelete
        )
    }

    QuestDetailScreenContent(
        state = state,
        objectivesState = objectivesState,
        rewardsState = rewardsState,
        consequencesState = consequencesState,
        notesState = notesState,
        relationsState = relationsState,
        historyState = historyState,
        sessionsState = sessionsState,
        onBack = onBack,
        onEditQuest = onEditQuest,
        onOpenQuest = onOpenQuest,
        onOpenSession = onOpenSession,
        actions = actions,
        objectiveActions = objectiveActions,
        rewardActions = rewardActions,
        consequenceActions = consequenceActions,
        noteActions = noteActions,
        relationActions = relationActions,
        sessionActions = sessionActions
    )
}

/**
 * The quest screen without its ViewModel, so it can be previewed and tested.
 *
 * One [LazyColumn] for the whole screen; the blocks below the status are
 * [com.example.digitaldungeon.ui.quest.components.objectivesSection] and its three siblings, which
 * contribute items to this one list rather than nesting lists of their own.
 *
 * The edit action is only offered once there is a quest: a form opened on a quest that is gone
 * would have nothing to fill itself from. The four blocks are drawn for the same reason - there is
 * nothing to hang them on without a quest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailScreenContent(
    state: QuestDetailUiState,
    objectivesState: QuestObjectivesUiState,
    rewardsState: QuestRewardsUiState,
    consequencesState: QuestConsequencesUiState,
    notesState: QuestNotesUiState,
    relationsState: QuestRelationsUiState = QuestRelationsUiState(),
    historyState: QuestHistoryUiState = QuestHistoryUiState(),
    sessionsState: QuestSessionsUiState = QuestSessionsUiState(),
    onBack: () -> Unit,
    onEditQuest: (Int) -> Unit,
    onOpenQuest: (Int) -> Unit = {},
    onOpenSession: (Int) -> Unit = {},
    actions: QuestDetailActions = QuestDetailActions(),
    objectiveActions: QuestObjectiveActions = QuestObjectiveActions(),
    rewardActions: QuestRewardActions = QuestRewardActions(),
    consequenceActions: QuestConsequenceActions = QuestConsequenceActions(),
    noteActions: QuestNoteActions = QuestNoteActions(),
    relationActions: QuestRelationActions = QuestRelationActions(),
    sessionActions: QuestSessionActions = QuestSessionActions()
) {

    val quest = state.quest

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        quest?.title
                            ?: stringResource(R.string.quest_detail_title_fallback)
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
                },
                actions = {
                    if (quest != null) {
                        TextButton(
                            onClick = { onEditQuest(quest.id) }
                        ) {
                            Text(stringResource(R.string.common_edit))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        // One branch for four blocks' confirmations, so two can never be on screen at once - the
        // ViewModel does not produce that, and written as a `when` it stays impossible rather than
        // merely unlikely. This is also the order of precedence if a state ever carried two.
        val pendingObjective = objectivesState.pendingDeletion
        val pendingReward = rewardsState.pendingDeletion
        val pendingConsequence = consequencesState.pendingDeletion
        val pendingNote = notesState.pendingDeletion
        val pendingRelation = relationsState.pendingDeletion
        val pendingSessionLink = sessionsState.pendingDeletion

        when {
            pendingObjective != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_objective_delete_dialog_title),
                text = stringResource(
                    R.string.quest_objective_delete_dialog_text,
                    pendingObjective.objectiveTitle
                ),
                onConfirm = objectiveActions.onConfirmDelete,
                onDismiss = objectiveActions.onCancelDelete
            )

            pendingReward != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_reward_delete_dialog_title),
                text = stringResource(
                    R.string.quest_reward_delete_dialog_text,
                    pendingReward.rewardTitle
                ),
                onConfirm = rewardActions.onConfirmDelete,
                onDismiss = rewardActions.onCancelDelete
            )

            pendingConsequence != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_consequence_delete_dialog_title),
                text = stringResource(
                    R.string.quest_consequence_delete_dialog_text,
                    pendingConsequence.consequenceTitle
                ),
                onConfirm = consequenceActions.onConfirmDelete,
                onDismiss = consequenceActions.onCancelDelete
            )

            // No name in the sentence: a note has no title, and its text is still readable behind
            // the dialog.
            pendingNote != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_note_delete_dialog_title),
                text = stringResource(R.string.quest_note_delete_dialog_text),
                onConfirm = noteActions.onConfirmDelete,
                onDismiss = noteActions.onCancelDelete
            )

            // "Remove" and not "Delete" on the confirm button: the link goes, both quests stay,
            // and the wording has to say which of the two is happening.
            pendingRelation != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_relation_delete_dialog_title),
                text = stringResource(
                    R.string.quest_relation_delete_dialog_text,
                    pendingRelation.otherQuestTitle.asString()
                ),
                onConfirm = relationActions.onConfirmDelete,
                onDismiss = relationActions.onCancelDelete,
                confirmLabel = stringResource(R.string.common_remove)
            )

            pendingSessionLink != null -> ConfirmDeleteDialog(
                title = stringResource(R.string.quest_session_unlink_dialog_title),
                text = stringResource(
                    R.string.quest_session_unlink_dialog_text,
                    pendingSessionLink.sessionTitle.asString()
                ),
                onConfirm = sessionActions.onConfirmDelete,
                onDismiss = sessionActions.onCancelDelete,
                confirmLabel = stringResource(R.string.common_remove)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Why the quest would not load, or why a status change failed. The null check stays
            // outside item {}: an item that renders nothing is still an item, and the list's
            // spacing would leave a blank gap for it.
            state.error?.let { error ->
                item {
                    InlineErrorText(error = error)
                }
            }

            if (state.isLoading) {
                item {
                    CircularProgressIndicator()
                }
            }

            // Deleted, here or from the list. Not an error - a statement about the journal.
            if (state.isMissing) {
                item {
                    Text(
                        text = stringResource(R.string.quest_not_found),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (quest != null) {

                item {
                    QuestFacts(quest = quest)
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    Text(
                        text = stringResource(R.string.quest_detail_status_heading),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.quest_detail_status_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    QuestStatusSelector(
                        selectedStatus = quest.status,
                        onStatusSelected = actions.onStatusSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // The order the quest is read in: what has to be done, what it pays, what it left
                // behind, and whatever else the DM wrote down about it.
                objectivesSection(objectivesState, objectiveActions)
                rewardsSection(rewardsState, rewardActions)
                consequencesSection(consequencesState, consequenceActions)
                notesSection(notesState, noteActions)
                relationsSection(relationsState, relationActions, onOpenQuest)
                questSessionsSection(sessionsState, sessionActions, onOpenSession)
                // Last, because it is the only block that is read rather than written.
                historySection(historyState)
            }
        }
    }
}

/**
 * Everything the quest states about itself, in the order it is read in: what it is about first,
 * then how it is filed, then when it was written down.
 *
 * The title is deliberately absent - it is already the top bar's, and repeating it as a heading
 * would say the same thing twice.
 */
@Composable
private fun QuestFacts(
    quest: Quest,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Said rather than left blank: an empty space does not tell the reader whether there is no
        // description or whether it failed to load.
        Text(
            text = quest.description.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.quest_detail_description_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = if (quest.description.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        if (quest.isHidden) {
            Text(
                text = stringResource(R.string.quest_detail_hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Text(
            text = stringResource(
                R.string.quest_detail_category,
                quest.category.label()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(
                R.string.quest_detail_priority,
                quest.priority.label()
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Only when the DM dated it. Most quests are never dated in world time.
        quest.inGameDate
            ?.takeIf { it.isNotBlank() }
            ?.let { date ->
                Text(
                    text = stringResource(R.string.quest_detail_in_game_date, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        // Provenance, and the only thing that tells two similarly named quests apart.
        CreatedAtLabel(createdAt = quest.createdAt)
    }
}

private val previewObjectives = listOf(
    QuestObjective(
        id = 1,
        questId = PREVIEW_QUEST.id,
        title = "Reach the Tser Pool before nightfall",
        description = "The road is open until dusk.",
        status = QuestObjectiveStatus.COMPLETED,
        orderIndex = 0,
        isOptional = false
    ),
    QuestObjective(
        id = 2,
        questId = PREVIEW_QUEST.id,
        title = "Get Ireena past the gates of Vallaki",
        description = "",
        status = QuestObjectiveStatus.NOT_STARTED,
        orderIndex = 1,
        isOptional = false
    ),
    QuestObjective(
        id = 3,
        questId = PREVIEW_QUEST.id,
        title = "Hear out the Vistani at the pool",
        description = "Madam Eva reads the cards, if asked.",
        status = QuestObjectiveStatus.NOT_STARTED,
        orderIndex = 2,
        isOptional = true
    )
)

private val previewRewards = listOf(
    QuestReward(
        id = 1,
        questId = PREVIEW_QUEST.id,
        title = "300 gold from Ismark",
        description = "Paid out of the burgomaster's estate.",
        type = QuestRewardType.MONEY
    ),
    QuestReward(
        id = 2,
        questId = PREVIEW_QUEST.id,
        title = "The Vistani owe the party a favour",
        description = "",
        type = QuestRewardType.REPUTATION
    )
)

private val previewConsequences = listOf(
    QuestConsequence(
        id = 1,
        questId = PREVIEW_QUEST.id,
        title = "Vallaki closes its gates at dusk",
        description = "The baron blames the party for drawing attention.",
        type = QuestConsequenceType.WORLD
    )
)

private val previewNotes = listOf(
    QuestNote(
        id = 1,
        questId = PREVIEW_QUEST.id,
        content = "Ireena will not be left behind, whatever the party decides.",
        createdAt = PREVIEW_CREATED_AT_EARLIER,
        updatedAt = PREVIEW_CREATED_AT_EARLIER
    ),
    QuestNote(
        id = 2,
        questId = PREVIEW_QUEST.id,
        content = "Reworded after session 5: she now insists on the church, not the inn.",
        createdAt = PREVIEW_CREATED_AT_EARLIER,
        updatedAt = PREVIEW_CREATED_AT
    )
)

@Composable
private fun PreviewQuestDetailScreen(
    state: QuestDetailUiState,
    objectivesState: QuestObjectivesUiState = QuestObjectivesUiState(),
    rewardsState: QuestRewardsUiState = QuestRewardsUiState(),
    consequencesState: QuestConsequencesUiState = QuestConsequencesUiState(),
    notesState: QuestNotesUiState = QuestNotesUiState(),
    relationsState: QuestRelationsUiState = QuestRelationsUiState(),
    historyState: QuestHistoryUiState = QuestHistoryUiState(),
    sessionsState: QuestSessionsUiState = QuestSessionsUiState()
) {
    DigitalDungeonTheme {
        QuestDetailScreenContent(
            state = state,
            objectivesState = objectivesState,
            rewardsState = rewardsState,
            consequencesState = consequencesState,
            notesState = notesState,
            relationsState = relationsState,
            historyState = historyState,
            sessionsState = sessionsState,
            onBack = {},
            onEditQuest = {},
            onOpenQuest = {},
            onOpenSession = {}
        )
    }
}

/** The whole screen with every block filled in - what a quest looks like once it has a history. */
@Preview(showBackground = true, heightDp = 2600)
@Composable
private fun QuestDetailScreenContentPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST.id,
            quest = PREVIEW_QUEST
        ),
        objectivesState = QuestObjectivesUiState(objectives = previewObjectives),
        rewardsState = QuestRewardsUiState(rewards = previewRewards),
        consequencesState = QuestConsequencesUiState(consequences = previewConsequences),
        notesState = QuestNotesUiState(notes = previewNotes)
    )
}

/**
 * A quest with nothing but a title and four empty blocks - which is a complete quest, and the
 * state every quest starts in.
 */
@Preview(name = "Minimal quest", showBackground = true, heightDp = 1800)
@Composable
private fun QuestDetailScreenContentMinimalPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST_MINIMAL.id,
            quest = PREVIEW_QUEST_MINIMAL
        )
    )
}

@Preview(name = "Hidden and failed", showBackground = true, heightDp = 1800)
@Composable
private fun QuestDetailScreenContentHiddenPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST_HIDDEN.id,
            quest = PREVIEW_QUEST_HIDDEN
        )
    )
}

@Preview(name = "Editing a reward", showBackground = true, heightDp = 2600)
@Composable
private fun QuestDetailScreenContentEditingRewardPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST.id,
            quest = PREVIEW_QUEST
        ),
        rewardsState = QuestRewardsUiState(
            rewards = previewRewards,
            editingRewardId = 1,
            formTitle = "300 gold from Ismark",
            formDescription = "Paid out of the burgomaster's estate.",
            formType = QuestRewardType.MONEY
        )
    )
}

@Preview(name = "Note refused", showBackground = true, heightDp = 2600)
@Composable
private fun QuestDetailScreenContentNoteRefusedPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST.id,
            quest = PREVIEW_QUEST
        ),
        notesState = QuestNotesUiState(
            notes = previewNotes,
            formContent = "   ",
            formError = UiText.of(R.string.quest_note_content_required)
        )
    )
}

@Preview(name = "Delete step dialog", showBackground = true, heightDp = 1800)
@Composable
private fun QuestDetailScreenContentDeletingObjectivePreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST.id,
            quest = PREVIEW_QUEST
        ),
        objectivesState = QuestObjectivesUiState(
            objectives = previewObjectives,
            pendingDeletion = PendingObjectiveDeletion(
                objectiveId = 1,
                objectiveTitle = "Reach the Tser Pool before nightfall"
            )
        )
    )
}

@Preview(name = "First load", showBackground = true)
@Composable
private fun QuestDetailScreenContentLoadingPreview() {
    // "Quest not found" must not appear here: nothing is known yet.
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = 1,
            isLoading = true
        ),
        objectivesState = QuestObjectivesUiState(isLoading = true),
        rewardsState = QuestRewardsUiState(isLoading = true),
        consequencesState = QuestConsequencesUiState(isLoading = true),
        notesState = QuestNotesUiState(isLoading = true)
    )
}

@Preview(name = "Quest is gone", showBackground = true)
@Composable
private fun QuestDetailScreenContentMissingPreview() {
    // No quest, so none of the four blocks either - there is nothing to hang them on.
    PreviewQuestDetailScreen(state = QuestDetailUiState(questId = 1))
}

/**
 * Every error channel at once, which is the point of there being five: each message sits next to
 * the block it is about, and none of them hides another.
 */
@Preview(name = "Every channel failed", showBackground = true, heightDp = 2000)
@Composable
private fun QuestDetailScreenContentErrorPreview() {
    PreviewQuestDetailScreen(
        state = QuestDetailUiState(
            questId = PREVIEW_QUEST.id,
            quest = PREVIEW_QUEST,
            error = UiText.of(R.string.quest_error_status_failed)
        ),
        objectivesState = QuestObjectivesUiState(
            error = UiText.of(R.string.quest_error_objectives_load_failed)
        ),
        rewardsState = QuestRewardsUiState(
            error = UiText.of(R.string.quest_error_rewards_load_failed)
        ),
        consequencesState = QuestConsequencesUiState(
            error = UiText.of(R.string.quest_error_consequences_load_failed)
        ),
        notesState = QuestNotesUiState(
            error = UiText.of(R.string.quest_error_notes_load_failed)
        )
    )
}
