package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.quest.state.QuestConsequencesUiState
import com.example.digitaldungeon.ui.quest.state.QuestNotesUiState
import com.example.digitaldungeon.ui.quest.state.QuestObjectivesUiState
import com.example.digitaldungeon.ui.quest.state.QuestRewardsUiState

/**
 * The blocks of the quest screen, each as a `LazyListScope` extension.
 *
 * Extensions and not composables, so every row is an item of the screen's one lazy list rather
 * than a nested list that cannot be measured in the same direction. In their own file because the
 * screen would otherwise be most of a thousand lines of items, and because the four of them share
 * one shape: a heading, an error channel of their own, a first-load spinner, a sentence for the
 * empty case, the rows, and the form that writes them.
 *
 * Each block keeps its own error next to itself. That is the whole reason the ViewModel carries one
 * state per block - a message about the rewards must not appear to be about the notes.
 */

/** A divider and a heading, which is how every block below announces itself. */
private fun LazyListScope.sectionHeading(
    textRes: Int,
    withDivider: Boolean = true
) {
    if (withDivider) {
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
    item {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

/**
 * The steps of the quest, in the order the DM put them in, with the form that writes them.
 */
fun LazyListScope.objectivesSection(
    objectivesState: QuestObjectivesUiState,
    actions: QuestObjectiveActions
) {

    sectionHeading(R.string.quest_objectives_heading, withDivider = false)

    // The null check stays outside item {}: an item that renders nothing is still an item, and the
    // list's spacing would leave a blank gap for it.
    objectivesState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (objectivesState.isLoading && objectivesState.objectives.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    // Said out loud rather than left as blank space: a quest without steps is a complete quest,
    // not a quest that is missing something.
    if (objectivesState.hasNoObjectives) {
        item { EmptyBlockText(R.string.quest_objectives_empty) }
    }

    itemsIndexed(
        items = objectivesState.objectives,
        key = { _, objective -> "objective-${objective.id}" }
    ) { index, objective ->

        QuestObjectiveRow(
            objective = objective,
            // One-based, because the list is read as "first this, then that".
            position = index + 1,
            canMoveUp = objectivesState.canMoveUp(index),
            canMoveDown = objectivesState.canMoveDown(index),
            onStatusSelected = { status -> actions.onStatusSelected(objective, status) },
            onEdit = { actions.onEditRequest(objective) },
            onMoveUp = { actions.onMoveUp(objective) },
            onMoveDown = { actions.onMoveDown(objective) },
            onDelete = { actions.onDeleteRequest(objective) }
        )
    }

    item {
        QuestObjectiveForm(
            title = objectivesState.formTitle,
            description = objectivesState.formDescription,
            isOptional = objectivesState.formIsOptional,
            isEditing = objectivesState.isEditing,
            onTitleChange = actions.onTitleChange,
            onDescriptionChange = actions.onDescriptionChange,
            onOptionalChange = actions.onOptionalChange,
            onAdd = actions.onAdd,
            onUpdate = actions.onUpdate,
            onCancelEdit = actions.onCancelEdit,
            modifier = Modifier.padding(top = 8.dp),
            error = objectivesState.formError
        )
    }
}

/** What the quest is worth, and the form that writes it. */
fun LazyListScope.rewardsSection(
    rewardsState: QuestRewardsUiState,
    actions: QuestRewardActions
) {

    sectionHeading(R.string.quest_rewards_heading)

    rewardsState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (rewardsState.isLoading && rewardsState.rewards.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (rewardsState.hasNoRewards) {
        item { EmptyBlockText(R.string.quest_rewards_empty) }
    }

    items(
        items = rewardsState.rewards,
        key = { reward -> "reward-${reward.id}" }
    ) { reward ->

        QuestEntryRow(
            title = reward.title,
            typeLabel = reward.type.label(),
            description = reward.description,
            onEdit = { actions.onEditRequest(reward) },
            onDelete = { actions.onDeleteRequest(reward) }
        )
    }

    item {
        QuestEntryForm(
            title = rewardsState.formTitle,
            description = rewardsState.formDescription,
            titleLabel = stringResource(R.string.quest_reward_title_label),
            descriptionLabel = stringResource(R.string.quest_reward_description_label),
            typeLabel = stringResource(R.string.quest_reward_type_label),
            addLabel = stringResource(R.string.quest_reward_add),
            updateLabel = stringResource(R.string.quest_reward_update),
            isEditing = rewardsState.isEditing,
            onTitleChange = actions.onTitleChange,
            onDescriptionChange = actions.onDescriptionChange,
            onAdd = actions.onAdd,
            onUpdate = actions.onUpdate,
            onCancelEdit = actions.onCancelEdit,
            modifier = Modifier.padding(top = 8.dp),
            error = rewardsState.formError
        ) {
            QuestRewardTypeSelector(
                selectedType = rewardsState.formType,
                onTypeSelected = actions.onTypeSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** What came of the quest, and the form that writes it. */
fun LazyListScope.consequencesSection(
    consequencesState: QuestConsequencesUiState,
    actions: QuestConsequenceActions
) {

    sectionHeading(R.string.quest_consequences_heading)

    consequencesState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (consequencesState.isLoading && consequencesState.consequences.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (consequencesState.hasNoConsequences) {
        item { EmptyBlockText(R.string.quest_consequences_empty) }
    }

    items(
        items = consequencesState.consequences,
        key = { consequence -> "consequence-${consequence.id}" }
    ) { consequence ->

        QuestEntryRow(
            title = consequence.title,
            typeLabel = consequence.type.label(),
            description = consequence.description,
            onEdit = { actions.onEditRequest(consequence) },
            onDelete = { actions.onDeleteRequest(consequence) }
        )
    }

    item {
        QuestEntryForm(
            title = consequencesState.formTitle,
            description = consequencesState.formDescription,
            titleLabel = stringResource(R.string.quest_consequence_title_label),
            descriptionLabel = stringResource(R.string.quest_consequence_description_label),
            typeLabel = stringResource(R.string.quest_consequence_type_label),
            addLabel = stringResource(R.string.quest_consequence_add),
            updateLabel = stringResource(R.string.quest_consequence_update),
            isEditing = consequencesState.isEditing,
            onTitleChange = actions.onTitleChange,
            onDescriptionChange = actions.onDescriptionChange,
            onAdd = actions.onAdd,
            onUpdate = actions.onUpdate,
            onCancelEdit = actions.onCancelEdit,
            modifier = Modifier.padding(top = 8.dp),
            error = consequencesState.formError
        ) {
            QuestConsequenceTypeSelector(
                selectedType = consequencesState.formType,
                onTypeSelected = actions.onTypeSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The DM's notes on the quest, newest first, and the form that writes them. */
fun LazyListScope.notesSection(
    notesState: QuestNotesUiState,
    actions: QuestNoteActions
) {

    sectionHeading(R.string.quest_notes_heading)

    notesState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (notesState.isLoading && notesState.notes.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (notesState.hasNoNotes) {
        item { EmptyBlockText(R.string.quest_notes_empty) }
    }

    items(
        items = notesState.notes,
        key = { note -> "note-${note.id}" }
    ) { note ->

        QuestNoteRow(
            note = note,
            onEdit = { actions.onEditRequest(note) },
            onDelete = { actions.onDeleteRequest(note) }
        )
    }

    item {
        QuestNoteForm(
            content = notesState.formContent,
            isEditing = notesState.isEditing,
            onContentChange = actions.onContentChange,
            onAdd = actions.onAdd,
            onUpdate = actions.onUpdate,
            onCancelEdit = actions.onCancelEdit,
            modifier = Modifier.padding(top = 8.dp),
            error = notesState.formError
        )
    }
}

/**
 * The one sentence a block says when it holds nothing.
 *
 * Quiet on purpose: every one of these states is a normal, complete state for a quest - plenty of
 * quests have no rewards written down and no notes - so the line reports a fact rather than
 * pointing out a gap.
 */
@Composable
private fun EmptyBlockText(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
