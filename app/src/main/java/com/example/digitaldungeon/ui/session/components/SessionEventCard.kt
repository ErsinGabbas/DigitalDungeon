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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One timeline entry.
 *
 * The card knows nothing about its position in the list — the screen owns that
 * and passes the result as [moveUp] / [moveDown]. That keeps the card reusable
 * (no index, no list size, no "is this the last one" logic inside) and lets the
 * screen switch reordering off wholesale, which is what happens while the
 * timeline is filtered.
 *
 * What the card does own is the explanation. [EventMoveAvailability] arrives with
 * the reason attached, so a greyed-out arrow carries it on the card — spelled out
 * in its `contentDescription` for a screen reader, and shown as a tooltip on long
 * press for everyone else. It used to be stated only in the status line above the
 * list, which left two unexplained dead buttons per card as soon as that line was
 * scrolled off screen.
 *
 * @param now the moment the stamp is read against, see [formatEventTimestamp].
 *   Required rather than defaulted to the clock: a card that read
 *   `System.currentTimeMillis()` itself would be impossible to skip during
 *   recomposition and impossible to preview reproducibly.
 * @param moveUp why the "up" arrow can or cannot be used.
 * @param moveDown the same for "down".
 */
@Composable
fun SessionEventCard(
    event: SessionEvent,
    now: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    moveUp: EventMoveAvailability = EventMoveAvailability.AT_EDGE,
    moveDown: EventMoveAvailability = EventMoveAvailability.AT_EDGE,
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
                // Type and time stay one bounded, unweighted group, so a long
                // title cannot push the stamp off the card
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.type.label(),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatEventTimestamp(
                            timestamp = event.timestamp,
                            now = now
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        // One step quieter than the type: the stamp is reference
                        // information, not the headline of the card
                        color = contentColor.copy(alpha = TIMESTAMP_ALPHA)
                    )
                }

                event.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        // Takes what is left next to the stamp instead of all of
                        // it, so a short title still sits on the right edge
                        modifier = Modifier.weight(1f, fill = false)
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
                        text = stringResource(R.string.event_participants_heading),
                        style = MaterialTheme.typography.labelSmall
                    )
                    event.participants.forEach { participant ->
                        Text(
                            // One format resource rather than a template: the
                            // abbreviations and their order are part of the language
                            text = stringResource(
                                R.string.event_participant_line,
                                participant.name,
                                participant.level,
                                participant.maxHp
                            ),
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
                    MoveEventButton(
                        icon = Icons.Filled.KeyboardArrowUp,
                        labels = moveUpLabels(moveUp),
                        enabled = moveUp.isAvailable,
                        onClick = onMoveUp
                    )
                    MoveEventButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        labels = moveDownLabels(moveDown),
                        enabled = moveDown.isAvailable,
                        onClick = onMoveDown
                    )
                }

                Row {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.session_action_edit))
                    }
                    Button(
                        onClick = onDelete
                    ) {
                        Text(stringResource(R.string.session_action_delete))
                    }
                }
            }
        }
    }
}

/** Keeps the stamp legible on every container color while staying secondary. */
private const val TIMESTAMP_ALPHA = 0.7f

/**
 * The two things an arrow has to say about itself.
 *
 * @param contentDescription what the arrow is for, and when it is unusable, why —
 *   a screen reader announces "dimmed" but has no idea what is dimming it.
 * @param blockedReason the sentence shown on long press, or `null` when the arrow
 *   works and there is nothing to excuse. Separate from [contentDescription]
 *   because this one is read instead of the card, not alongside it: it is a full
 *   explanation with the way out in it, not a label for a control.
 */
private data class MoveLabels(
    val contentDescription: String,
    val blockedReason: String?
)

@Composable
private fun moveUpLabels(availability: EventMoveAvailability): MoveLabels =
    when (availability) {
        EventMoveAvailability.AVAILABLE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_up),
            blockedReason = null
        )

        EventMoveAvailability.AT_EDGE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_up_first),
            blockedReason = stringResource(R.string.event_reorder_blocked_first)
        )

        EventMoveAvailability.FILTER_ACTIVE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_up_filtered),
            blockedReason = stringResource(R.string.event_reorder_blocked_by_filter)
        )
    }

@Composable
private fun moveDownLabels(availability: EventMoveAvailability): MoveLabels =
    when (availability) {
        EventMoveAvailability.AVAILABLE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_down),
            blockedReason = null
        )

        EventMoveAvailability.AT_EDGE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_down_last),
            blockedReason = stringResource(R.string.event_reorder_blocked_last)
        )

        EventMoveAvailability.FILTER_ACTIVE -> MoveLabels(
            contentDescription = stringResource(R.string.event_action_move_down_filtered),
            blockedReason = stringResource(R.string.event_reorder_blocked_by_filter)
        )
    }

/**
 * One reorder arrow, disabled when it cannot be used and able to say why.
 *
 * Disabled rather than hidden: the pair keeps its place, so turning a filter on
 * does not reshuffle every card's action row, and the control the user is looking
 * for is where they left it. What makes that honest instead of broken is the
 * tooltip — [MoveLabels.blockedReason] is in reach on the card itself, which is
 * where the question "why is this greyed out" is actually asked.
 *
 * The tooltip is only attached when there is something to explain, so a working
 * arrow does not swallow long presses for a message that would say nothing. Its
 * long press is picked up on the `Initial` pointer pass by `TooltipBox`, before
 * the `IconButton` beneath it, which is what makes the gesture work on a button
 * that is disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveEventButton(
    icon: ImageVector,
    labels: MoveLabels,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val button: @Composable () -> Unit = {
        IconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(
                imageVector = icon,
                contentDescription = labels.contentDescription
            )
        }
    }

    val blockedReason = labels.blockedReason
    if (blockedReason == null) {
        button()
        return
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(blockedReason)
            }
        },
        state = rememberTooltipState(),
        // The message is already in the arrow's contentDescription, so a screen
        // reader must not have to find the popup as well
        focusable = false
    ) {
        button()
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
 *
 * Every card is rendered against the fixed [previewNow] instead of the real
 * clock, so the stamps are identical on every run: the first four events belong
 * to "today" and show a time only, the last one is eight days old and shows its
 * date as well.
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
                    now = previewNow,
                    onEdit = {},
                    onDelete = {},
                    moveUp = EventMoveAvailability.of(
                        reorderingEnabled = reorderingEnabled,
                        isAtEdge = index == 0
                    ),
                    moveDown = EventMoveAvailability.of(
                        reorderingEnabled = reorderingEnabled,
                        isAtEdge = index == previewAllEvents.lastIndex
                    ),
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

/**
 * How the timeline looks while a filter is on: every arrow is disabled, and
 * every one of them can say why on long press.
 */
@Preview(name = "SessionEventCard - reordering off", showBackground = true, heightDp = 1200)
@Composable
private fun SessionEventCardReorderingDisabledPreview() {
    DigitalDungeonTheme {
        SessionEventCardGallery(reorderingEnabled = false)
    }
}

/**
 * The two things the card gained, on one narrow screen: the stamp next to the
 * type, and a participant list that is a snapshot — "Sera" is still named by
 * this month-old event although the campaign no longer has her.
 */
@Preview(name = "SessionEventCard - stamp and snapshot", showBackground = true, widthDp = 320)
@Composable
private fun SessionEventCardSnapshotPreview() {
    DigitalDungeonTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionEventCard(
                    event = previewCombatEvent,
                    now = previewNow,
                    onEdit = {},
                    onDelete = {},
                    moveUp = EventMoveAvailability.AT_EDGE,
                    moveDown = EventMoveAvailability.AVAILABLE
                )
                SessionEventCard(
                    event = previewDepartedMemberEvent,
                    now = previewNow,
                    onEdit = {},
                    onDelete = {},
                    moveUp = EventMoveAvailability.AVAILABLE,
                    moveDown = EventMoveAvailability.AT_EDGE
                )
            }
        }
    }
}
