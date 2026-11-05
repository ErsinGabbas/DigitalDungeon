package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

@Composable
fun QuickNoteInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    canAdd: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = { Text("Quick note...") },
            singleLine = true
        )
        Button(
            onClick = onAdd,
            // Adding a blank note is a no-op in the ViewModel, so the button
            // must not look like it does something
            enabled = canAdd
        ) {
            Text("Add")
        }
    }
}

@Preview(name = "QuickNoteInput", showBackground = true)
@Composable
private fun QuickNoteInputPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickNoteInput(
                text = "",
                onTextChange = {},
                onAdd = {},
                canAdd = false
            )
            QuickNoteInput(
                text = "   ",
                onTextChange = {},
                onAdd = {},
                canAdd = false
            )
            QuickNoteInput(
                text = "The bell rings again",
                onTextChange = {},
                onAdd = {},
                canAdd = true
            )
        }
    }
}
