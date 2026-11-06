package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Type filter for the timeline.
 *
 * Close relative of [EventTypeSelector], but a different job: this one has an
 * "All" state ([selectedType] `null`) and tapping the active chip clears the
 * filter, while the selector always has exactly one type picked.
 */
@Composable
fun EventTypeFilterRow(
    selectedType: SessionEventType?,
    onTypeSelected: (SessionEventType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text(stringResource(R.string.event_type_filter_all)) }
        )

        SessionEventType.entries.forEach { type ->
            val isSelected = type == selectedType
            FilterChip(
                selected = isSelected,
                // Tapping the active chip is the shortest way back to "All"
                onClick = { onTypeSelected(if (isSelected) null else type) },
                label = { Text(type.label()) }
            )
        }
    }
}

@Preview(name = "EventTypeFilterRow", showBackground = true)
@Composable
private fun EventTypeFilterRowPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EventTypeFilterRow(
                selectedType = null,
                onTypeSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            SessionEventType.entries.forEach { selected ->
                EventTypeFilterRow(
                    selectedType = selected,
                    onTypeSelected = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
