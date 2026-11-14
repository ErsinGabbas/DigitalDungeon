package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestSessionLink
import com.example.digitaldungeon.ui.session.state.SessionQuestEntry
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The quests recorded as having come up in this session, each a way into the quest itself.
 *
 * Read-only on purpose. A quest is linked from the quest's own screen, where the campaign's quests
 * are already on hand and the rule about both ends being in one campaign is enforced; putting a
 * second picker here would mean two places to maintain and two places to get that rule wrong. The
 * sentence shown when the block is empty says where the linking happens.
 *
 * Sits above the timeline rather than below it, because the timeline is the part of this screen
 * that grows without bound: a block under a weighted list would be pushed off the bottom as soon as
 * the session had a few events in it.
 */
@Composable
fun SessionQuestsSection(
    entries: List<SessionQuestEntry>,
    hasNoLinkedQuests: Boolean,
    onOpenQuest: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Text(
            text = stringResource(R.string.session_quests_heading),
            style = MaterialTheme.typography.titleSmall
        )

        if (hasNoLinkedQuests) {
            Text(
                text = stringResource(R.string.session_quests_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            return@Column
        }

        entries.forEach { entry ->
            TextButton(
                onClick = { onOpenQuest(entry.questId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.questTitle
                        ?: stringResource(R.string.session_quests_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun entry(id: Int, questId: Int, title: String?) = SessionQuestEntry(
    link = QuestSessionLink(id = id, questId = questId, sessionId = 1),
    questId = questId,
    questTitle = title
)

@Preview(name = "SessionQuestsSection", showBackground = true, widthDp = 400)
@Composable
private fun SessionQuestsSectionPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SessionQuestsSection(
                entries = listOf(
                    entry(1, 10, "Escort Ireena to Vallaki"),
                    entry(2, 11, "The miller's missing daughter"),
                    // The far end has not landed yet, so the row says so instead of being blank.
                    entry(3, 12, null)
                ),
                hasNoLinkedQuests = false,
                onOpenQuest = {}
            )

            SessionQuestsSection(
                entries = emptyList(),
                hasNoLinkedQuests = true,
                onOpenQuest = {}
            )
        }
    }
}
