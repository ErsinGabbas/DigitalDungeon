package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        title = { Text("Delete event?") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Show what exactly is about to go, so the user can tell
                // whether they hit the row they meant to
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = event.deletionSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Title when the event has one, otherwise the beginning of its content —
 * quick notes are created without a title.
 */
private fun SessionEvent.deletionSummary(): String {
    val title = title?.takeIf { it.isNotBlank() }
    if (title != null) return title

    val content = content.trim()
    if (content.isEmpty()) return "(empty event)"

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
