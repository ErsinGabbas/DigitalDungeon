package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One timeline entry.
 *
 * The card knows nothing about its position in the list — the screen owns that
 * and passes the result as [canMoveUp] / [canMoveDown]. That keeps the card
 * reusable (no index, no list size, no "is this the last one" logic inside) and
 * lets the screen switch reordering off wholesale, which is what happens while
 * the timeline is filtered.
 */
@Composable
fun SessionEventCard(
    event: SessionEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    val containerColor = event.type.containerColor()
    val contentColor = event.type.contentColor()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.labelSmall
                )
                event.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Text(
                text = event.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (event.participants.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = "Participants:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    event.participants.forEach { participant ->
                        Text(
                            text = "${participant.name} (Lvl ${participant.level}, HP: ${participant.maxHp})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Icons, not labelled buttons: Edit and Delete already take the
                // width this card has to spare
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        // Disabled rather than a silent no-op at the edges of
                        // the timeline, otherwise the button looks broken
                        enabled = canMoveUp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Move event up"
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Move event down"
                        )
                    }
                }

                Row {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Edit")
                    }
                    Button(
                        onClick = onDelete
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionEventType.containerColor(): Color =
    when (this) {
        SessionEventType.SCENE -> MaterialTheme.colorScheme.primaryContainer
        SessionEventType.COMBAT -> MaterialTheme.colorScheme.errorContainer
        SessionEventType.DIALOGUE -> MaterialTheme.colorScheme.secondaryContainer
        SessionEventType.DISCOVERY -> MaterialTheme.colorScheme.tertiaryContainer
        SessionEventType.NOTE -> MaterialTheme.colorScheme.surfaceVariant
    }

/**
 * Content color must match the container role, otherwise text on the error,
 * secondary, tertiary and surface variants is painted with the primary
 * container's "on" color and can lose contrast.
 */
@Composable
private fun SessionEventType.contentColor(): Color =
    when (this) {
        SessionEventType.SCENE -> MaterialTheme.colorScheme.onPrimaryContainer
        SessionEventType.COMBAT -> MaterialTheme.colorScheme.onErrorContainer
        SessionEventType.DIALOGUE -> MaterialTheme.colorScheme.onSecondaryContainer
        SessionEventType.DISCOVERY -> MaterialTheme.colorScheme.onTertiaryContainer
        SessionEventType.NOTE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/**
 * Every event type at once — the five container roles are where text contrast
 * breaks first, so they are reviewed side by side in both themes.
 *
 * The first and last card also show the disabled edges of the reorder controls.
 */
@Composable
private fun SessionEventCardGallery(reorderingEnabled: Boolean = true) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            previewAllEvents.forEachIndexed { index, event ->
                SessionEventCard(
                    event = event,
                    onEdit = {},
                    onDelete = {},
                    canMoveUp = reorderingEnabled && index > 0,
                    canMoveDown = reorderingEnabled && index < previewAllEvents.lastIndex,
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }
    }
}

@Preview(name = "SessionEventCard - light", showBackground = true, heightDp = 1200)
@Composable
private fun SessionEventCardLightPreview() {
    DigitalDungeonTheme(darkTheme = false) {
        SessionEventCardGallery()
    }
}

@Preview(name = "SessionEventCard - dark", showBackground = true, heightDp = 1200)
@Composable
private fun SessionEventCardDarkPreview() {
    DigitalDungeonTheme(darkTheme = true) {
        SessionEventCardGallery()
    }
}

/** How the timeline looks while a filter is on: every arrow is disabled. */
@Preview(name = "SessionEventCard - reordering off", showBackground = true, heightDp = 1200)
@Composable
private fun SessionEventCardReorderingDisabledPreview() {
    DigitalDungeonTheme {
        SessionEventCardGallery(reorderingEnabled = false)
    }
}
