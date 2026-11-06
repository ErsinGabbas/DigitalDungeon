package com.example.digitaldungeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders an epoch-millis instant as a date in the user's locale.
 *
 * [DateFormat.MEDIUM] on purpose: it spells the month out ("Nov 6, 2026"), so the
 * value reads the same to everyone, while the all-numeric [DateFormat.SHORT] would
 * turn 06/11 into either November 6th or June 11th depending on who is looking.
 *
 * Platform formatting rather than a pattern of our own: the order of the fields,
 * the separators and the month names all come from the locale instead of from a
 * hardcoded `"dd MMM yyyy"` that would be wrong in half the world.
 *
 * @param locale defaults to the system locale; passed explicitly by tests.
 */
fun formatCreationDate(
    epochMillis: Long,
    locale: Locale = Locale.getDefault()
): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        .format(Date(epochMillis))

/**
 * "Created <date>" line for a card that stands for something stored - a campaign,
 * a session. Deliberately quiet: it is provenance, not content, and must not
 * compete with the name above it.
 *
 * @param createdAt epoch millis, as stored in the database.
 * @param labelled whether to wrap the date in the "Created <date>" sentence;
 * pass false for the bare date. A format resource and not a prefix glued in
 * front of the date, because not every language puts the label first.
 */
@Composable
fun CreatedAtLabel(
    createdAt: Long,
    modifier: Modifier = Modifier,
    labelled: Boolean = true,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {

    // Keyed on the configuration, so that changing the device language reformats
    // the date instead of leaving the previous locale's spelling on screen.
    val configuration = LocalConfiguration.current

    val date = remember(createdAt, configuration) {
        formatCreationDate(createdAt)
    }

    Text(
        text = if (labelled) stringResource(R.string.common_created_at, date) else date,
        style = style,
        color = color,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun CreatedAtLabelPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fixed instants, never System.currentTimeMillis(): a preview that
            // renders a different string on every pass is no reference at all.
            CreatedAtLabel(createdAt = PREVIEW_CREATED_AT)
            CreatedAtLabel(createdAt = PREVIEW_CREATED_AT_EARLIER)
            CreatedAtLabel(createdAt = PREVIEW_CREATED_AT, labelled = false)
        }
    }
}
