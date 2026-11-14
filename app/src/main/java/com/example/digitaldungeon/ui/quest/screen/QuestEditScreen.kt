package com.example.digitaldungeon.ui.quest.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.components.SwitchRow
import com.example.digitaldungeon.ui.quest.components.QuestCategorySelector
import com.example.digitaldungeon.ui.quest.components.QuestEditActions
import com.example.digitaldungeon.ui.quest.components.QuestPrioritySelector
import com.example.digitaldungeon.ui.quest.components.QuestStatusSelector
import com.example.digitaldungeon.ui.quest.state.QuestEditUiState
import com.example.digitaldungeon.ui.quest.viewmodel.QuestEditViewModel
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The form that writes a quest, with its ViewModel attached - the entry point the navigation graph
 * calls for both `quest/new/{campaignId}` and `quest/{questId}/edit`.
 *
 * Which of the two it is is decided by the route arguments the ViewModel reads, not by a parameter
 * here: the screen looks the same either way.
 *
 * @param onSaved the write went through and the screen is done. Navigation, so it comes from the
 * graph - the form has no opinion on where the user ends up.
 */
@Composable
fun QuestEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: QuestEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keyed on the flag and not on the whole state, so this fires once per successful save. The
    // flag lives in the state rather than in an event channel, which is what makes a rotation
    // mid-save unable to leave the user on a form that has already written its quest.
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
        }
    }

    val actions = remember(viewModel) {
        QuestEditActions(
            onTitleChange = viewModel::onTitleChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCategorySelected = viewModel::onCategorySelected,
            onStatusSelected = viewModel::onStatusSelected,
            onPrioritySelected = viewModel::onPrioritySelected,
            onHiddenChange = viewModel::onHiddenChange,
            onInGameDateChange = viewModel::onInGameDateChange,
            onToggleOptionalFields = viewModel::onToggleOptionalFields,
            onSave = viewModel::onSave
        )
    }

    QuestEditScreenContent(
        state = state,
        onBack = onBack,
        actions = actions
    )
}

/**
 * The form without its ViewModel, so it can be previewed and tested.
 *
 * Three fields are always on screen - the title, the description and the category - and that is
 * the whole promise of this module: writing down "the miller wants his daughter back" is one step.
 * The status, the priority, the hidden flag and the in-world date all have honest defaults, so
 * they sit behind one line the user can unfold
 * ([QuestEditUiState.areOptionalFieldsVisible]) instead of being asked about every time.
 *
 * The save button stays enabled on an empty title on purpose, the same way the campaign edit
 * dialog's does: a button that silently does nothing explains nothing, so pressing it produces the
 * reason instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditScreenContent(
    state: QuestEditUiState,
    onBack: () -> Unit,
    actions: QuestEditActions = QuestEditActions()
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) {
                                R.string.quest_edit_title
                            } else {
                                R.string.quest_create_title
                            }
                        )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Why the write failed, or that the quest being edited is gone.
            state.error?.let { error ->
                InlineErrorText(error = error)
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = actions.onTitleChange,
                label = {
                    Text(stringResource(R.string.quest_title_label))
                },
                singleLine = true,
                isError = state.titleError != null,
                modifier = Modifier.fillMaxWidth()
            )

            // Right under the field it is about, where the campaign dialog puts its own.
            state.titleError?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = actions.onDescriptionChange,
                label = {
                    Text(stringResource(R.string.quest_description_label))
                },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.quest_category_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            QuestCategorySelector(
                selectedCategory = state.category,
                onCategorySelected = actions.onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(
                onClick = actions.onToggleOptionalFields
            ) {
                Text(
                    stringResource(
                        if (state.areOptionalFieldsVisible) {
                            R.string.quest_optional_hide
                        } else {
                            R.string.quest_optional_show
                        }
                    )
                )
            }

            if (state.areOptionalFieldsVisible) {

                Text(
                    text = stringResource(R.string.quest_status_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuestStatusSelector(
                    selectedStatus = state.status,
                    onStatusSelected = actions.onStatusSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.quest_priority_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuestPrioritySelector(
                    selectedPriority = state.priority,
                    onPrioritySelected = actions.onPrioritySelected,
                    modifier = Modifier.fillMaxWidth()
                )

                SwitchRow(
                    label = stringResource(R.string.quest_hidden_label),
                    checked = state.isHidden,
                    onCheckedChange = actions.onHiddenChange
                )

                OutlinedTextField(
                    value = state.inGameDate,
                    onValueChange = actions.onInGameDateChange,
                    label = {
                        Text(stringResource(R.string.quest_in_game_date_label))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = actions.onSave,
                // An empty title does not disable it - pressing it produces the reason. See
                // QuestEditUiState.isSaveEnabled for what does.
                enabled = state.isSaveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

@Composable
private fun PreviewQuestEditScreen(state: QuestEditUiState) {
    DigitalDungeonTheme {
        QuestEditScreenContent(
            state = state,
            onBack = {}
        )
    }
}

/** A new quest: three fields and nothing else, which is the point. */
@Preview(showBackground = true)
@Composable
private fun QuestEditScreenContentNewPreview() {
    PreviewQuestEditScreen(QuestEditUiState())
}

@Preview(name = "New quest, fields unfolded", showBackground = true)
@Composable
private fun QuestEditScreenContentNewExpandedPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            title = "The miller's missing daughter",
            areOptionalFieldsVisible = true
        )
    )
}

@Preview(name = "Editing, unfolded", showBackground = true)
@Composable
private fun QuestEditScreenContentEditingPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            isEditing = true,
            title = "Bring Ireena to Vallaki",
            description = "Ismark asks the party to escort his sister past the Tser Pool.",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = false,
            inGameDate = "14th of Neth, 735 BC",
            areOptionalFieldsVisible = true
        )
    )
}

@Preview(name = "Refused empty title", showBackground = true)
@Composable
private fun QuestEditScreenContentRejectedPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            title = "   ",
            titleError = UiText.of(R.string.quest_title_required)
        )
    )
}

@Preview(name = "Saving", showBackground = true)
@Composable
private fun QuestEditScreenContentSavingPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            title = "Find the amber shard",
            isSaving = true
        )
    )
}

/** The quest was deleted between the list and this form: the save button is off. */
@Preview(name = "Quest is gone", showBackground = true)
@Composable
private fun QuestEditScreenContentMissingPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            isEditing = true,
            isMissing = true,
            error = UiText.of(R.string.quest_not_found)
        )
    )
}

@Preview(name = "Write failed", showBackground = true)
@Composable
private fun QuestEditScreenContentErrorPreview() {
    PreviewQuestEditScreen(
        QuestEditUiState(
            isEditing = true,
            title = "Find the amber shard",
            error = UiText.of(R.string.quest_error_update_failed)
        )
    )
}
