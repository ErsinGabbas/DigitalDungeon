package com.example.digitaldungeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * A labelled on/off switch where the **whole row** is the control.
 *
 * Written as one component because the obvious arrangement - a `Text` next to a `Switch` - is
 * subtly wrong twice over. Tapping the label does nothing, even though a sentence with a switch
 * at the end of it reads like one thing and is aimed at like one thing; and a screen reader
 * announces two unrelated nodes, a line of text and an unlabelled toggle, instead of "optional
 * step, off, switch".
 *
 * `Modifier.toggleable` with [Role.Switch] on the row is what fixes both: it merges the label and
 * the switch into a single node that carries the state and the action. The [Switch] itself is
 * handed `onCheckedChange = null` on purpose - it then draws the state and stays out of the
 * gesture, which is what keeps one tap from being handled twice.
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun SwitchRowPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SwitchRow(
                label = "Optional step",
                checked = false,
                onCheckedChange = {}
            )
            SwitchRow(
                label = "The party does not know about it yet",
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}
