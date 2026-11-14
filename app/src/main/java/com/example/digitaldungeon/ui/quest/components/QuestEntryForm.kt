package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Add / edit form for a titled, typed and described thing: a reward or a consequence.
 *
 * The counterpart of [QuestEntryRow] and shared for the same reason. The labels arrive as already
 * resolved strings and the kind selector arrives as a [typeSelector] slot, which is what keeps this
 * free of both enums - the caller drops in `QuestRewardTypeSelector` or
 * `QuestConsequenceTypeSelector` and the form never learns which it got.
 *
 * One form for adding and editing, switched by [isEditing], like every other editor in this app.
 * The confirm button stays enabled on an empty title on purpose: pressing it produces the reason,
 * and a button that silently does nothing explains nothing.
 */
@Composable
fun QuestEntryForm(
    title: String,
    description: String,
    titleLabel: String,
    descriptionLabel: String,
    typeLabel: String,
    addLabel: String,
    updateLabel: String,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAdd: () -> Unit,
    onUpdate: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    typeSelector: @Composable () -> Unit
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = {
                Text(titleLabel)
            },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = {
                Text(descriptionLabel)
            },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        typeSelector()

        error?.let { message ->
            Text(
                text = message.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditing) {
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(updateLabel)
                }
                Button(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(addLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestEntryFormPreview() {
    DigitalDungeonTheme {
        QuestEntryForm(
            title = "",
            description = "",
            titleLabel = stringResource(R.string.quest_reward_title_label),
            descriptionLabel = stringResource(R.string.quest_reward_description_label),
            typeLabel = stringResource(R.string.quest_reward_type_label),
            addLabel = stringResource(R.string.quest_reward_add),
            updateLabel = stringResource(R.string.quest_reward_update),
            isEditing = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {},
            modifier = Modifier.padding(16.dp)
        ) {
            QuestRewardTypeSelector(
                selectedType = QuestRewardType.MONEY,
                onTypeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Editing, refused empty title", showBackground = true, widthDp = 400)
@Composable
private fun QuestEntryFormEditingPreview() {
    DigitalDungeonTheme {
        QuestEntryForm(
            title = "   ",
            description = "Paid out of the burgomaster's estate.",
            titleLabel = stringResource(R.string.quest_reward_title_label),
            descriptionLabel = stringResource(R.string.quest_reward_description_label),
            typeLabel = stringResource(R.string.quest_reward_type_label),
            addLabel = stringResource(R.string.quest_reward_add),
            updateLabel = stringResource(R.string.quest_reward_update),
            isEditing = true,
            onTitleChange = {},
            onDescriptionChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {},
            modifier = Modifier.padding(16.dp),
            error = UiText.of(R.string.quest_reward_title_required)
        ) {
            QuestRewardTypeSelector(
                selectedType = QuestRewardType.ITEM,
                onTypeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
