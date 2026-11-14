package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.CreatedAtLabel
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One quest as a row of the quest list.
 *
 * The status comes first under the title, because that is the question the list is read to answer.
 * The category and the priority follow as quiet lines - they are how the list is filtered and
 * sorted, so they have to be visible, but they are not what the row is about.
 *
 * The description is clamped to two lines: a quest with three paragraphs of background must not
 * push the next quest off the screen. The whole text is one tap away.
 *
 * @param onOpen the card was tapped - the caller is expected to open the quest.
 * @param onDelete asks for the quest to be deleted; the caller confirms first.
 */
@Composable
fun QuestCard(
    quest: Quest,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        // `Modifier.clickable` and not the clickable `Card` overload, which is what SessionCard
        // does as well - one way of making a card tappable in this app, not two.
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onDelete
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }

            Text(
                text = quest.status.label(),
                style = MaterialTheme.typography.labelLarge,
                color = quest.status.color()
            )

            // Only when set: saying "the party knows about this one" on every other row would
            // make the normal case look like a statement.
            if (quest.isHidden) {
                Text(
                    text = stringResource(R.string.quest_detail_hidden),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Text(
                text = stringResource(
                    R.string.quest_detail_category,
                    quest.category.label()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(
                    R.string.quest_detail_priority,
                    quest.priority.label()
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (quest.description.isNotBlank()) {
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Provenance, like on a campaign or session card: it is the only thing that tells two
            // similarly named quests apart.
            CreatedAtLabel(createdAt = quest.createdAt)
        }
    }
}

/**
 * The colour a status is drawn in.
 *
 * Not decoration: a list of twenty quests is scanned, not read, and "which of these are dead" has
 * to survive that. The label is always there in words as well, so the colour only reinforces what
 * the row already says - nothing is encoded in colour alone.
 *
 * Every value comes from the theme's own roles, so both schemes stay consistent: a quest in play
 * takes the accent the rest of the app uses for what matters, one that ended badly takes the error
 * role, and the two waiting states stay as quiet as the lines below them.
 */
@Composable
private fun QuestStatus.color(): Color =
    when (this) {
        QuestStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        QuestStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        QuestStatus.FAILED,
        QuestStatus.CANCELLED -> MaterialTheme.colorScheme.error

        QuestStatus.NOT_STARTED,
        QuestStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun QuestCardPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PREVIEW_QUESTS.forEach { quest ->
                QuestCard(
                    quest = quest,
                    onOpen = {},
                    onDelete = {}
                )
            }
        }
    }
}

/**
 * Every status next to each other, which is the only way to check that the colours tell them
 * apart - and that they still do in the dark scheme.
 */
@Preview(name = "Every status", showBackground = true, widthDp = 360)
@Composable
private fun QuestCardEveryStatusPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestStatus.entries.forEach { status ->
                QuestCard(
                    quest = PREVIEW_QUEST_MINIMAL.copy(status = status),
                    onOpen = {},
                    onDelete = {}
                )
            }
        }
    }
}
