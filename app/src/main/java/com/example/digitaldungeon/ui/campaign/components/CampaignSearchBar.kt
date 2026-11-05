package com.example.digitaldungeon.ui.campaign.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Filters the campaign list as the user types.
 *
 * Stateless: the query lives in the ViewModel state, so the filter survives a
 * rotation and the list and the query can never disagree.
 */
@Composable
fun CampaignSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = {
            Text("Search campaigns")
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
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun CampaignSearchBarPreview() {
    DigitalDungeonTheme {
        CampaignSearchBar(
            query = "strahd",
            onQueryChange = {},
            onClearQuery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Empty query", showBackground = true)
@Composable
private fun CampaignSearchBarEmptyPreview() {
    DigitalDungeonTheme {
        CampaignSearchBar(
            query = "",
            onQueryChange = {},
            onClearQuery = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
