package com.example.digitaldungeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Renames something that has a short name and a longer free text next to it - a
 * campaign (name / description) or a session (title / notes).
 *
 * Deliberately stateless, like [ConfirmDeleteDialog]: whoever shows it owns both
 * the "what is being edited" and the typed values, so a rotation cannot throw a
 * half typed name away.
 *
 * @param error reason the last save attempt was refused, shown above the buttons.
 * A [UiText] and not a [String]: it comes from a ViewModel, which cannot know the
 * language, so it is formatted here where it is drawn. The confirm button stays
 * enabled on purpose - a button that silently does nothing explains nothing.
 */
@Composable
fun EditDetailsDialog(
    title: String,
    nameValue: String,
    nameLabel: String,
    detailsValue: String,
    detailsLabel: String,
    onNameChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    confirmLabel: String = stringResource(R.string.common_save),
    dismissLabel: String = stringResource(R.string.common_cancel)
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = nameValue,
                    onValueChange = onNameChange,
                    label = {
                        Text(nameLabel)
                    },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = detailsValue,
                    onValueChange = onDetailsChange,
                    label = {
                        Text(detailsLabel)
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { message ->
                    Text(
                        text = message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(dismissLabel)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditDetailsDialogPreview() {
    DigitalDungeonTheme {
        EditDetailsDialog(
            title = stringResource(R.string.campaign_edit_dialog_title),
            nameValue = "Curse of Strahd",
            nameLabel = stringResource(R.string.campaign_name_label),
            detailsValue = "Gothic horror in the valley of Barovia",
            detailsLabel = stringResource(R.string.campaign_description_label),
            onNameChange = {},
            onDetailsChange = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Rejected empty name", showBackground = true)
@Composable
private fun EditDetailsDialogWithErrorPreview() {
    DigitalDungeonTheme {
        EditDetailsDialog(
            title = stringResource(R.string.campaign_session_edit_dialog_title),
            nameValue = "",
            nameLabel = stringResource(R.string.campaign_session_title_label),
            detailsValue = "The party reaches the Death House",
            detailsLabel = stringResource(R.string.campaign_session_notes_label),
            onNameChange = {},
            onDetailsChange = {},
            onConfirm = {},
            onDismiss = {},
            error = UiText.of(R.string.campaign_session_title_required)
        )
    }
}
