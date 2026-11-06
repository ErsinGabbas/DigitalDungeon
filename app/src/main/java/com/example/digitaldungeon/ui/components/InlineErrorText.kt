package com.example.digitaldungeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * A reason something was refused or failed, shown in place next to what caused
 * it.
 *
 * One composable rather than the same three lines repeated per error channel:
 * the campaign detail screen shows three of these at once - campaign, participants
 * and sessions each own their own failure - and they must be told apart by *what
 * they say*, never by one of them having drifted to a different size or colour.
 *
 * Takes a [UiText] and not a [String] on purpose: the message is formatted here,
 * at draw time, so it is in the language being read rather than the one that was
 * current when the failure happened.
 */
@Composable
fun InlineErrorText(
    error: UiText,
    modifier: Modifier = Modifier
) {

    Text(
        text = error.asString(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun InlineErrorTextPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            InlineErrorText(
                error = UiText.of(R.string.campaign_not_found)
            )

            // With arguments, which is the case a formatted-too-early string
            // would have got wrong.
            InlineErrorText(
                error = UiText.of(
                    R.string.campaign_participant_level_out_of_range,
                    1,
                    100
                )
            )

            InlineErrorText(
                error = UiText.Raw("SQLiteConstraintException: FOREIGN KEY failed")
            )
        }
    }
}
