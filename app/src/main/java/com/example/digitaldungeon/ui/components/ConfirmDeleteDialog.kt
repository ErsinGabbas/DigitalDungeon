package com.example.digitaldungeon.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Confirmation step in front of a destructive action.
 *
 * Deliberately stateless: whoever shows it owns the "what is about to be deleted"
 * state, so the question survives configuration changes instead of living in a
 * `remember` that a screen rotation would throw away.
 *
 * [title] and [text] are already formatted strings - the caller names the thing
 * being deleted, so only the caller can resolve the sentence that mentions it.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(R.string.common_delete),
    dismissLabel: String = stringResource(R.string.common_cancel)
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = text)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
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
private fun ConfirmDeleteDialogPreview() {
    DigitalDungeonTheme {
        ConfirmDeleteDialog(
            title = stringResource(R.string.campaign_delete_dialog_title),
            text = stringResource(
                R.string.campaign_delete_dialog_text,
                "Curse of Strahd"
            ),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
