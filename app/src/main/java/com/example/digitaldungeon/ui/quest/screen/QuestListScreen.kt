package com.example.digitaldungeon.ui.quest.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.quest.components.PREVIEW_QUESTS
import com.example.digitaldungeon.ui.quest.components.QuestCard
import com.example.digitaldungeon.ui.quest.components.QuestCategoryFilterRow
import com.example.digitaldungeon.ui.quest.components.QuestFilterStatusLine
import com.example.digitaldungeon.ui.quest.components.QuestListActions
import com.example.digitaldungeon.ui.quest.components.QuestSearchBar
import com.example.digitaldungeon.ui.quest.components.QuestSortRow
import com.example.digitaldungeon.ui.quest.components.QuestStatusFilterRow
import com.example.digitaldungeon.ui.quest.state.PendingQuestDeletion
import com.example.digitaldungeon.ui.quest.state.QuestListUiState
import com.example.digitaldungeon.ui.quest.state.QuestSort
import com.example.digitaldungeon.ui.quest.viewmodel.QuestListViewModel
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The quest list of one campaign with its ViewModel attached - the entry point the navigation
 * graph calls.
 *
 * A thin wrapper by design: it subscribes to the state and binds the nine callbacks. The campaign
 * id is not a parameter here - it reaches the ViewModel through `SavedStateHandle`, the same way
 * every other id in this app does.
 */
@Composable
fun QuestListScreen(
    onBack: () -> Unit,
    onOpenQuest: (Int) -> Unit,
    onCreateQuest: () -> Unit,
    viewModel: QuestListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val actions = remember(viewModel) {
        QuestListActions(
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onClearSearchQuery = viewModel::onClearSearchQuery,
            onStatusSelected = viewModel::onStatusFilterSelected,
            onCategorySelected = viewModel::onCategoryFilterSelected,
            onSortSelected = viewModel::onSortSelected,
            onClearFilters = viewModel::onClearFilters,
            onDeleteRequest = viewModel::onDeleteRequest,
            onConfirmDelete = viewModel::onConfirmDelete,
            onCancelDelete = viewModel::onCancelDelete
        )
    }

    QuestListScreenContent(
        state = state,
        onBack = onBack,
        onOpenQuest = onOpenQuest,
        onCreateQuest = onCreateQuest,
        actions = actions
    )
}

/**
 * The quest list without its ViewModel, so it can be previewed and tested.
 *
 * Everything on screen is a function of [state]: which of the two empty-list messages is shown,
 * whether the filter summary is there, whether the confirmation dialog is open. No
 * `hiltViewModel()`, no remembered decision a caller cannot reproduce.
 *
 * One [LazyColumn] for the whole screen, with the form and the filters as items of it rather than
 * a `Column` above a list: a campaign with two hundred quests is the reason this is lazy, and
 * wrapping it in a `verticalScroll` would throw that away.
 *
 * @param onBack the navigate-up button.
 * @param onOpenQuest a card was tapped, by quest id.
 * @param onCreateQuest the "new quest" button. All three are navigation, which is why none of them
 * is part of [QuestListActions].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestListScreenContent(
    state: QuestListUiState,
    onBack: () -> Unit,
    onOpenQuest: (Int) -> Unit,
    onCreateQuest: () -> Unit,
    actions: QuestListActions = QuestListActions()
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.quest_list_title))
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

        val pendingDeletion = state.pendingDeletion

        if (pendingDeletion != null) {
            ConfirmDeleteDialog(
                title = stringResource(R.string.quest_delete_dialog_title),
                // The title goes in as an argument: a translation decides where in the sentence
                // it belongs.
                text = stringResource(
                    R.string.quest_delete_dialog_text,
                    pendingDeletion.questTitle
                ),
                onConfirm = actions.onConfirmDelete,
                onDismiss = actions.onCancelDelete
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Why the last load or delete failed. At the top, where it cannot be scrolled past.
            // The null check stays outside item {}: an item that renders nothing is still an item,
            // and the list's spacing would leave a blank gap for it.
            state.error?.let { error ->
                item {
                    InlineErrorText(error = error)
                }
            }

            item {
                Button(
                    onClick = onCreateQuest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.quest_create_action))
                }
            }

            item {
                QuestSearchBar(
                    query = state.searchQuery,
                    onQueryChange = actions.onSearchQueryChange,
                    onClearQuery = actions.onClearSearchQuery
                )
            }

            item {
                QuestStatusFilterRow(
                    selectedStatus = state.statusFilter,
                    onStatusSelected = actions.onStatusSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                QuestCategoryFilterRow(
                    selectedCategory = state.categoryFilter,
                    onCategorySelected = actions.onCategorySelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = stringResource(R.string.quest_sort_heading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                QuestSortRow(
                    selectedSort = state.sort,
                    onSortSelected = actions.onSortSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Both only while something is actually hidden: on an unfiltered list the summary
            // would state the obvious and the reset button would have nothing to reset.
            if (state.isFilterActive) {
                item {
                    QuestFilterStatusLine(
                        visibleCount = state.visibleQuests.size,
                        totalCount = state.quests.size
                    )
                }

                item {
                    TextButton(
                        onClick = actions.onClearFilters
                    ) {
                        Text(stringResource(R.string.quest_action_clear_filters))
                    }
                }
            }

            if (state.isLoading && state.quests.isEmpty()) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }

            // "Nothing matched" is not "nothing written down yet" - the two say different things
            // and only one of them is the user's own doing.
            if (state.hasNoQuests) {
                item {
                    Text(
                        text = stringResource(R.string.quest_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (state.hasNoMatches) {
                item {
                    Text(
                        text = stringResource(R.string.quest_no_matches),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(
                items = state.visibleQuests,
                key = { quest -> quest.id }
            ) { quest ->

                QuestCard(
                    quest = quest,
                    onOpen = {
                        onOpenQuest(quest.id)
                    },
                    onDelete = {
                        actions.onDeleteRequest(quest)
                    }
                )
            }
        }
    }
}

/**
 * Renders [state] with every callback a no-op, so each preview below is one line of state and
 * nothing else - which is also how a test calls the screen when it only cares about what is drawn.
 */
@Composable
private fun PreviewQuestListScreen(state: QuestListUiState) {
    DigitalDungeonTheme {
        QuestListScreenContent(
            state = state,
            onBack = {},
            onOpenQuest = {},
            onCreateQuest = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuestListScreenContentPreview() {
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            quests = PREVIEW_QUESTS
        )
    )
}

@Preview(name = "No quests yet", showBackground = true)
@Composable
private fun QuestListScreenContentEmptyPreview() {
    PreviewQuestListScreen(QuestListUiState(campaignId = 1))
}

@Preview(name = "First load", showBackground = true)
@Composable
private fun QuestListScreenContentLoadingPreview() {
    // Neither empty-list message may appear here: nothing is known yet, so claiming the campaign
    // has no quests would be a guess.
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            isLoading = true
        )
    )
}

@Preview(name = "Filtered", showBackground = true)
@Composable
private fun QuestListScreenContentFilteredPreview() {
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            quests = PREVIEW_QUESTS,
            statusFilter = QuestStatus.ACTIVE,
            sort = QuestSort.NEWEST
        )
    )
}

@Preview(name = "Filters hid everything", showBackground = true)
@Composable
private fun QuestListScreenContentNoMatchesPreview() {
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            quests = PREVIEW_QUESTS,
            categoryFilter = QuestCategory.PERSONAL,
            searchQuery = "dragon"
        )
    )
}

@Preview(name = "Quest list failed", showBackground = true)
@Composable
private fun QuestListScreenContentErrorPreview() {
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            quests = PREVIEW_QUESTS,
            error = UiText.of(R.string.quest_error_load_failed)
        )
    )
}

@Preview(name = "Delete dialog open", showBackground = true)
@Composable
private fun QuestListScreenContentDeletingPreview() {
    PreviewQuestListScreen(
        QuestListUiState(
            campaignId = 1,
            quests = PREVIEW_QUESTS,
            pendingDeletion = PendingQuestDeletion(
                questId = 1,
                questTitle = "Bring Ireena to Vallaki"
            )
        )
    )
}
