package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Picks the cast of a single event out of [options] (in practice the campaign
 * roster).
 *
 * Multiple choice, and choosing nobody is a normal answer — most quick notes
 * name no one. There is therefore no "done" or "clear" action: the chips are the
 * whole control.
 *
 * Renders **nothing at all** when [options] is empty. The guard lives here and
 * not at the call sites so a campaign without participants can never produce a
 * labelled but empty block.
 *
 * Selection is by value: a chip is on when an equal [EventParticipant] is in
 * [selected]. The caller stores exactly the objects handed to [onToggle], which
 * is what makes the result a snapshot — nothing about the event points back at
 * the roster afterwards. Two roster entries that are equal in every field are
 * indistinguishable here and act as one chip; they are already indistinguishable
 * to the reader of the event as well.
 *
 * @param options everyone who can be picked, in the order they are shown.
 * @param selected the current choice; entries not in [options] are simply not
 *   drawn, so callers that must keep them visible have to offer them as options.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventParticipantPicker(
    options: List<EventParticipant>,
    selected: List<EventParticipant>,
    onToggle: (EventParticipant) -> Unit,
    modifier: Modifier = Modifier,
    // A default argument of a composable is evaluated in composition, so the label
    // follows the locale like every other string on the screen
    label: String = stringResource(R.string.event_participants_label)
) {
    if (options.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )

        // Wrapping, not horizontally scrollable: inside a dialog a row that
        // scrolls sideways hides part of the party behind an edge. FlowRow is
        // not a lazy layout, so this stays safe inside the scrolling containers
        // the screen already uses.
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { participant ->
                val isSelected = participant in selected

                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(participant) },
                    label = { Text(participant.name) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                // The chip already reports its selected state,
                                // so the icon must not say it a second time
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun EventParticipantPickerGallery() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // The usual state of a fresh quick note: a party, nobody picked
            EventParticipantPicker(
                options = previewCampaignParticipants,
                selected = emptyList(),
                onToggle = {}
            )

            EventParticipantPicker(
                options = previewCampaignParticipants,
                selected = previewCampaignParticipants.take(2),
                onToggle = {}
            )

            // What the edit dialog offers for an event naming someone who has
            // since left the campaign: the departed member is still an option,
            // still selected, and only leaves the event if actually unchecked
            EventParticipantPicker(
                options = previewCampaignParticipants + previewDepartedParticipant,
                selected = listOf(previewDepartedParticipant),
                onToggle = {},
                label = stringResource(R.string.event_participants_label_snapshot)
            )
        }
    }
}

@Preview(name = "EventParticipantPicker - light", showBackground = true)
@Composable
private fun EventParticipantPickerLightPreview() {
    DigitalDungeonTheme(darkTheme = false) {
        EventParticipantPickerGallery()
    }
}

@Preview(name = "EventParticipantPicker - dark", showBackground = true)
@Composable
private fun EventParticipantPickerDarkPreview() {
    DigitalDungeonTheme(darkTheme = true) {
        EventParticipantPickerGallery()
    }
}

/** A long roster, to check that the chips wrap instead of being cut off. */
@Preview(name = "EventParticipantPicker - wrapping", showBackground = true, widthDp = 320)
@Composable
private fun EventParticipantPickerWrappingPreview() {
    DigitalDungeonTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventParticipantPicker(
                options = previewLargeParty,
                selected = previewLargeParty.filterIndexed { index, _ -> index % 2 == 0 },
                onToggle = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
