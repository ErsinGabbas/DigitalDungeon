package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Explains, in one line above the timeline, both surprises of a filtered view:
 * the arrows are greyed out, and the share action still exports everything.
 *
 * Only ever the overview. It scrolls out of reach as soon as the list is long,
 * which is why the arrows no longer depend on it to explain themselves — each one
 * carries its own reason (see [EventMoveAvailability]). This line says the thing
 * no single card can: how much of the timeline is being hidden.
 *
 * A component of its own rather than a private function of the screen, so a test
 * can put the sentence in front of a counter without building a whole screen.
 *
 * @param visibleCount how many events survived the filter.
 * @param totalCount how many exist in total.
 */
@Composable
fun FilterStatusLine(
    visibleCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        // One format resource instead of concatenated halves: the sentence is built
        // differently in other languages, and a plural because "event(s)" has more
        // than two forms in some of them. The count the grammar follows is the total.
        text = pluralStringResource(
            R.plurals.session_filter_status,
            totalCount,
            visibleCount,
            totalCount
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

/**
 * The two counts that read differently: one event out of one, and a handful out
 * of many — the second is where the plural rules of a language show up.
 */
@Preview(name = "FilterStatusLine", showBackground = true, widthDp = 360)
@Composable
private fun FilterStatusLinePreview() {
    DigitalDungeonTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                FilterStatusLine(visibleCount = 1, totalCount = 1)
                FilterStatusLine(visibleCount = 3, totalCount = 21)
            }
        }
    }
}
