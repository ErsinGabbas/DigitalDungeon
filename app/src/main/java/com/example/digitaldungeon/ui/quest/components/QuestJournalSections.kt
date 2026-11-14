package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.CreatedAtLabel
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT_EARLIER
import com.example.digitaldungeon.ui.quest.state.QuestHistoryUiState
import com.example.digitaldungeon.ui.quest.state.QuestSessionsUiState
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The two blocks that tie a quest to the rest of the journal: what was done to it, and which
 * sessions it came up in.
 */

/**
 * The sentence one history entry reads as.
 *
 * The log stores data and not prose - a status constant for a status move, an objective's title for
 * the rest - so the sentence is assembled here, at render time, in whatever language the app is
 * being read in. A stored sentence would be frozen in the language it was written in, which is the
 * same reason `UiText` exists.
 *
 * A status name this build does not recognise falls through as the raw stored value rather than
 * being hidden: a line that says `Status: ARCHIVED` is odd but true, and an empty line is neither.
 */
@Composable
fun QuestHistoryEntry.asSentence(): String {

    val subject = when (eventType) {
        QuestHistoryEventType.QUEST_STATUS_CHANGED ->
            QuestStatus.entries
                .firstOrNull { it.name == description }
                ?.label()
                ?: description

        QuestHistoryEventType.OBJECTIVE_COMPLETED,
        QuestHistoryEventType.OBJECTIVE_FAILED,
        QuestHistoryEventType.OBJECTIVE_REOPENED -> description
    }

    val frame = when (eventType) {
        QuestHistoryEventType.QUEST_STATUS_CHANGED -> R.string.quest_history_status_changed
        QuestHistoryEventType.OBJECTIVE_COMPLETED -> R.string.quest_history_objective_completed
        QuestHistoryEventType.OBJECTIVE_FAILED -> R.string.quest_history_objective_failed
        QuestHistoryEventType.OBJECTIVE_REOPENED -> R.string.quest_history_objective_reopened
    }

    return stringResource(frame, subject)
}

/**
 * What was done to the quest, newest first.
 *
 * Read-only, and there is nothing on this screen that writes to it: entries are appended by the
 * repositories that make the change. That is also why the block has no form, no edit and no
 * delete - a history that could be rewritten would not be worth reading.
 */
fun LazyListScope.historySection(
    historyState: QuestHistoryUiState
) {

    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }

    item {
        Text(
            text = stringResource(R.string.quest_history_heading),
            style = MaterialTheme.typography.titleSmall
        )
    }

    historyState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (historyState.isLoading && historyState.entries.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (historyState.hasNoEntries) {
        item {
            Text(
                text = stringResource(R.string.quest_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    items(
        items = historyState.entries,
        key = { entry -> entry.id }
    ) { entry ->

        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = entry.asSentence(),
                style = MaterialTheme.typography.bodyMedium
            )

            // The date of the entry, drawn the same way every other provenance line in the app is.
            CreatedAtLabel(
                createdAt = entry.timestamp,
                labelled = false
            )
        }
    }
}

/**
 * The sessions the quest came up in, with the picker that records another one.
 *
 * Each row opens the session, which is the other half of the crossing the plan asks for: from a
 * session to its quests, and from a quest back to its sessions.
 */
fun LazyListScope.questSessionsSection(
    sessionsState: QuestSessionsUiState,
    actions: QuestSessionActions,
    onOpenSession: (Int) -> Unit
) {

    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }

    item {
        Text(
            text = stringResource(R.string.quest_sessions_heading),
            style = MaterialTheme.typography.titleSmall
        )
    }

    sessionsState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (sessionsState.isLoading && sessionsState.links.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (sessionsState.hasNoSessions) {
        item {
            Text(
                text = stringResource(R.string.quest_sessions_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    items(
        items = sessionsState.entries,
        key = { entry -> entry.link.id }
    ) { entry ->

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = { onOpenSession(entry.sessionId) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.sessionTitle
                        ?: stringResource(R.string.quest_session_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TextButton(onClick = { actions.onDeleteRequest(entry) }) {
                Text(stringResource(R.string.common_remove))
            }
        }
    }

    item {
        if (sessionsState.hasNoCandidates) {
            Text(
                text = stringResource(R.string.quest_sessions_no_candidates),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = stringResource(R.string.quest_session_picker_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                QuestPickerButton(
                    label = sessionsState.selectedSession?.title
                        ?: stringResource(R.string.quest_session_picker_placeholder),
                    options = sessionsState.candidates.map { session ->
                        session.title to { actions.onSessionSelected(session.id) }
                    }
                )

                sessionsState.formError?.let { message ->
                    Text(
                        text = message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = actions.onAdd,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.quest_session_add))
                }
            }
        }
    }
}

private val previewHistory = listOf(
    QuestHistoryEntry(
        id = 3,
        questId = 1,
        eventType = QuestHistoryEventType.QUEST_STATUS_CHANGED,
        description = QuestStatus.COMPLETED.name,
        timestamp = PREVIEW_CREATED_AT
    ),
    QuestHistoryEntry(
        id = 2,
        questId = 1,
        eventType = QuestHistoryEventType.OBJECTIVE_COMPLETED,
        description = "Reach the Tser Pool before nightfall",
        timestamp = PREVIEW_CREATED_AT_EARLIER
    ),
    QuestHistoryEntry(
        id = 1,
        questId = 1,
        eventType = QuestHistoryEventType.OBJECTIVE_REOPENED,
        description = "Get Ireena past the gates of Vallaki",
        timestamp = PREVIEW_CREATED_AT_EARLIER
    )
)

/**
 * The four sentences the log can read as, including a status constant this build does not know -
 * which falls through as the raw value rather than vanishing.
 */
@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestHistorySentencesPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewHistory.forEach { entry ->
                Text(
                    text = entry.asSentence(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = QuestHistoryEntry(
                    id = 4,
                    questId = 1,
                    eventType = QuestHistoryEventType.QUEST_STATUS_CHANGED,
                    description = "ARCHIVED",
                    timestamp = PREVIEW_CREATED_AT
                ).asSentence(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
