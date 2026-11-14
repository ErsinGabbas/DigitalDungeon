package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.ui.components.CreatedAtLabel
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER
import com.example.digitaldungeon.ui.components.formatCreationDate
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One of the DM's notes on a quest.
 *
 * The text comes first and in full - no clamping, unlike a quest card's description: this screen
 * is where the note is read, so hiding half of it behind an ellipsis would defeat the point.
 *
 * The "edited" line appears only on a note whose [QuestNote.updatedAt] has moved past its
 * [QuestNote.createdAt]. On an untouched note the two are equal by construction, so saying
 * "edited" there would be a lie about the only thing those two fields exist to tell apart.
 */
@Composable
fun QuestNoteRow(
    note: QuestNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium
            )

            CreatedAtLabel(createdAt = note.createdAt)

            if (note.updatedAt > note.createdAt) {

                // Keyed on the configuration, so changing the device language reformats the date
                // instead of leaving the previous locale's spelling on screen - the same reason
                // CreatedAtLabel does it.
                val configuration = LocalConfiguration.current
                val editedOn = remember(note.updatedAt, configuration) {
                    formatCreationDate(note.updatedAt)
                }

                Text(
                    text = stringResource(R.string.quest_note_edited, editedOn),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.common_edit))
                }

                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

/**
 * Add / edit form for a note: one text area and nothing else, because a note is one thought.
 *
 * Multi-line and roomy on purpose - this is where a paragraph gets written, not a label - and the
 * confirm button stays enabled on empty text, like every other form here.
 */
@Composable
fun QuestNoteForm(
    content: String,
    isEditing: Boolean,
    onContentChange: (String) -> Unit,
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
            value = content,
            onValueChange = onContentChange,
            label = {
                Text(stringResource(R.string.quest_note_content_label))
            },
            minLines = 3,
            maxLines = 8,
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
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
                    Text(stringResource(R.string.quest_note_update))
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
                    Text(stringResource(R.string.quest_note_add))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestNoteRowPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Untouched: the two timestamps are equal, so no "edited" line.
            QuestNoteRow(
                note = QuestNote(
                    id = 1,
                    questId = 1,
                    content = "The miller is lying about the well.",
                    createdAt = PREVIEW_CREATED_AT_EARLIER,
                    updatedAt = PREVIEW_CREATED_AT_EARLIER
                ),
                onEdit = {},
                onDelete = {}
            )
            // Revised later, which is the one case the second timestamp exists for.
            QuestNoteRow(
                note = QuestNote(
                    id = 2,
                    questId = 1,
                    content = "He was not lying - the well really is dry, and the daughter " +
                        "went down it on her own.",
                    createdAt = PREVIEW_CREATED_AT_EARLIER,
                    updatedAt = PREVIEW_CREATED_AT
                ),
                onEdit = {},
                onDelete = {}
            )
        }
    }
}

@Preview(name = "Note form", showBackground = true, widthDp = 400)
@Composable
private fun QuestNoteFormPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuestNoteForm(
                content = "",
                isEditing = false,
                onContentChange = {},
                onAdd = {},
                onUpdate = {},
                onCancelEdit = {}
            )
            QuestNoteForm(
                content = "   ",
                isEditing = true,
                onContentChange = {},
                onAdd = {},
                onUpdate = {},
                onCancelEdit = {},
                error = UiText.of(R.string.quest_note_content_required)
            )
        }
    }
}
