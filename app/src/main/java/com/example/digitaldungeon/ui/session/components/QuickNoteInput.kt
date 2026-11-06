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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The one rule in the app for "is this quick note worth adding".
 *
 * Blank means blank: empty, spaces, tabs, a pasted newline. Whitespace-only text
 * would store an event with nothing in it, so it counts as nothing.
 *
 * Deliberately a plain function next to the component that enforces it, and the
 * same function
 * [SessionDetailUiState.canAddQuickNote][com.example.digitaldungeon.ui.session.state.SessionDetailUiState.canAddQuickNote]
 * is defined by — which is what the ViewModel guards `onAddQuickNote` with. One
 * definition, three users, so the button, the state and the guard cannot drift
 * apart into three slightly different ideas of "empty".
 */
fun isQuickNoteAddable(text: String): Boolean = text.isNotBlank()

/**
 * Text field plus Add button for the quick note.
 *
 * The emptiness rule is **not** a parameter. The component decides it from the
 * [text] it is already given, via [isQuickNoteAddable]: a caller cannot hand in a
 * disagreeing verdict, so the promise "a blank note cannot be added, and the
 * button does not pretend otherwise" is one this component actually keeps rather
 * than one it inherits from whoever called it. Before, `canAdd = text.isNotEmpty()`
 * from a caller was enough to light up Add on a line of spaces and have [onAdd]
 * fired for it — the ViewModel then dropped it silently, and the button had lied.
 *
 * There is no second reason to refuse a note in this screen (the cast plays no
 * part — see [isQuickNoteAddable]), so no `enabled` flag is offered either. If one
 * ever appears, it belongs here as an extra condition combined with this rule,
 * never as a replacement for it.
 *
 * @param onAdd called only for text that [isQuickNoteAddable] accepts.
 */
@Composable
fun QuickNoteInput(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAdd = isQuickNoteAddable(text)

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            placeholder = { Text(stringResource(R.string.session_quick_note_placeholder)) },
            singleLine = true
        )
        Button(
            // Disabled, so blank text cannot reach onAdd through a tap, and the
            // button does not look like it does something it will not do
            onClick = onAdd,
            enabled = canAdd
        ) {
            Text(stringResource(R.string.session_action_add))
        }
    }
}

/**
 * The three states of the Add button, decided by the text alone: empty, nothing
 * but spaces, and something worth keeping. Only the last one is enabled, and no
 * preview can arrange otherwise.
 */
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
                onAdd = {}
            )
            QuickNoteInput(
                text = "   ",
                onTextChange = {},
                onAdd = {}
            )
            QuickNoteInput(
                text = "The bell rings again",
                onTextChange = {},
                onAdd = {}
            )
        }
    }
}
