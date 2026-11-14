package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Filters the quest list as the user types, over the title and the description.
 *
 * Stateless: the query lives in the ViewModel state, so the filter survives a rotation and the
 * list and the query can never disagree.
 *
 * Deliberately its own component rather than a reuse of the campaign or session search field: the
 * three are near identical today, but turning them into one shared widget means rewriting two
 * working screens for the sake of this one, and per-feature components is the shape the rest of
 * this UI already has.
 */
@Composable
fun QuestSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = {
            Text(stringResource(R.string.quest_search_label))
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            // Only offered when there is something to clear.
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.quest_search_clear)
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun QuestSearchBarPreview() {
    DigitalDungeonTheme {
        QuestSearchBar(
            query = "miller",
            onQueryChange = {},
            onClearQuery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Empty query", showBackground = true)
@Composable
private fun QuestSearchBarEmptyPreview() {
    DigitalDungeonTheme {
        QuestSearchBar(
            query = "",
            onQueryChange = {},
            onClearQuery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
