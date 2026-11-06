package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Confirmation for an irreversible event deletion.
 *
 * Deliberately local to the session UI: the shared dialog in `ui/components`
 * is owned by another part of the app and is not reused here yet.
 */
@Composable
fun DeleteEventConfirmDialog(
    event: SessionEvent,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_delete_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.event_delete_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Show what exactly is about to go, so the user can tell
                // whether they hit the row they meant to
                Text(
                    text = event.type.label(),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = event.deletionSummary(
                        emptyPlaceholder = stringResource(R.string.event_delete_empty_summary)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.session_action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.session_action_cancel))
            }
        }
    )
}

/**
 * Title when the event has one, otherwise the beginning of its content —
 * quick notes are created without a title.
 *
 * [emptyPlaceholder] is handed in rather than read here, which keeps this a plain
 * function: the only localized part of the summary is the stand-in for an event with
 * nothing in it at all.
 */
private fun SessionEvent.deletionSummary(emptyPlaceholder: String): String {
    val title = title?.takeIf { it.isNotBlank() }
    if (title != null) return title

    val content = content.trim()
    if (content.isEmpty()) return emptyPlaceholder

    return if (content.length > SUMMARY_MAX_LENGTH) {
        content.take(SUMMARY_MAX_LENGTH).trimEnd() + "…"
    } else {
        content
    }
}

private const val SUMMARY_MAX_LENGTH = 120

@Preview(name = "Delete dialog - titled event", showBackground = true)
@Composable
private fun DeleteEventConfirmDialogPreview() {
    DigitalDungeonTheme {
        DeleteEventConfirmDialog(
            event = previewCombatEvent,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Delete dialog - untitled note", showBackground = true)
@Composable
private fun DeleteEventConfirmDialogUntitledPreview() {
    DigitalDungeonTheme {
        DeleteEventConfirmDialog(
            event = previewNoteEvent,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
