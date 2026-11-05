package com.example.digitaldungeon.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Confirmation step in front of a destructive action.
 *
 * Deliberately stateless: whoever shows it owns the "what is about to be deleted"
 * state, so the question survives configuration changes instead of living in a
 * `remember` that a screen rotation would throw away.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "Delete",
    dismissLabel: String = "Cancel"
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
            title = "Delete campaign?",
            text = "\"Curse of Strahd\" and all of its sessions and session events " +
                "will be deleted permanently. This cannot be undone.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
