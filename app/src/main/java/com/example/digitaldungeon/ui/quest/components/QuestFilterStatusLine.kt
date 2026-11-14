package com.example.digitaldungeon.ui.quest.components

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
 * Says, in one line above the list, how much of it the search and the filters are hiding.
 *
 * The thing no single row can say: a list of three quests looks complete, and only this line
 * tells the reader that there are twenty more behind the filters.
 *
 * @param visibleCount how many quests survived the filters.
 * @param totalCount how many the campaign has in total.
 */
@Composable
fun QuestFilterStatusLine(
    visibleCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Text(
        // One format resource instead of concatenated halves: the sentence is built differently
        // in other languages, and a plural because the noun after "of" has more than two forms in
        // some of them. The count the grammar follows is the total.
        text = pluralStringResource(
            R.plurals.quest_filter_status,
            totalCount,
            visibleCount,
            totalCount
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * The two counts that read differently: one quest out of one, and a handful out of many - the
 * second is where the plural rules of a language show up.
 */
@Preview(name = "QuestFilterStatusLine", showBackground = true, widthDp = 360)
@Composable
private fun QuestFilterStatusLinePreview() {
    DigitalDungeonTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuestFilterStatusLine(visibleCount = 1, totalCount = 1)
                QuestFilterStatusLine(visibleCount = 3, totalCount = 21)
            }
        }
    }
}
