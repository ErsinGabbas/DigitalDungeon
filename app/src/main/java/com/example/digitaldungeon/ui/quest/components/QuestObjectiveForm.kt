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
import com.example.digitaldungeon.ui.components.SwitchRow
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Add / edit form for one objective.
 *
 * One form for both jobs, switched by [isEditing], exactly like the participant editor on the
 * campaign screen: the fields are the same, and keeping two forms in step would be work for
 * nothing.
 *
 * Purely presentational - the ViewModel owns the values and decides whether they are acceptable,
 * so a refusal can be shown next to the form instead of the button quietly doing nothing.
 *
 * @param error why the last attempt was refused, shown above the buttons. The confirm button stays
 * enabled on purpose, like everywhere else in this app.
 */
@Composable
fun QuestObjectiveForm(
    title: String,
    description: String,
    isOptional: Boolean,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onOptionalChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onUpdate: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = {
                Text(stringResource(R.string.quest_objective_title_label))
            },
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = {
                Text(stringResource(R.string.quest_objective_description_label))
            },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        SwitchRow(
            label = stringResource(R.string.quest_objective_optional_label),
            checked = isOptional,
            onCheckedChange = onOptionalChange
        )

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
                    Text(stringResource(R.string.quest_objective_update))
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
                    Text(stringResource(R.string.quest_objective_add))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestObjectiveFormPreview() {
    DigitalDungeonTheme {
        QuestObjectiveForm(
            title = "",
            description = "",
            isOptional = false,
            isEditing = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onOptionalChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Editing an optional step", showBackground = true, widthDp = 400)
@Composable
private fun QuestObjectiveFormEditingPreview() {
    DigitalDungeonTheme {
        QuestObjectiveForm(
            title = "Recover the icon from the church",
            description = "Father Donavich knows where it was hidden.",
            isOptional = true,
            isEditing = true,
            onTitleChange = {},
            onDescriptionChange = {},
            onOptionalChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Refused empty objective", showBackground = true, widthDp = 400)
@Composable
private fun QuestObjectiveFormRefusedPreview() {
    DigitalDungeonTheme {
        QuestObjectiveForm(
            title = "   ",
            description = "",
            isOptional = false,
            isEditing = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onOptionalChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {},
            modifier = Modifier.padding(16.dp),
            error = UiText.of(R.string.quest_objective_title_required)
        )
    }
}
