package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Add / edit form for a campaign participant.
 *
 * Purely presentational - the ViewModel owns the field values and decides
 * whether the input is acceptable, so the refusal reason can be shown next to
 * the form instead of the button doing nothing.
 */
@Composable
fun ParticipantForm(
    name: String,
    level: String,
    maxHp: String,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onMaxHpChange: (String) -> Unit,
    onAdd: () -> Unit,
    onUpdate: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TextField(
                value = level,
                onValueChange = onLevelChange,
                label = { Text("Lvl") },
                modifier = Modifier.width(60.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
            TextField(
                value = maxHp,
                onValueChange = onMaxHpChange,
                label = { Text("HP") },
                modifier = Modifier.width(60.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
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
                    Text("Update Participant")
                }
                Button(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Participant")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ParticipantFormPreview() {
    DigitalDungeonTheme {
        ParticipantForm(
            name = "Ireena Kolyana",
            level = "4",
            maxHp = "28",
            isEditing = false,
            onNameChange = {},
            onLevelChange = {},
            onMaxHpChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ParticipantFormEditingPreview() {
    DigitalDungeonTheme {
        ParticipantForm(
            name = "Ismark",
            level = "3",
            maxHp = "24",
            isEditing = true,
            onNameChange = {},
            onLevelChange = {},
            onMaxHpChange = {},
            onAdd = {},
            onUpdate = {},
            onCancelEdit = {}
        )
    }
}
