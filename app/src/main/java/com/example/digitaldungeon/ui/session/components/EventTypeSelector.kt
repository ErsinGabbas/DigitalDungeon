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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

@Composable
fun EventTypeSelector(
    selectedType: SessionEventType,
    onTypeSelected: (SessionEventType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SessionEventType.entries.forEach { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(type.label())
                }
            )
        }
    }
}

@Preview(name = "EventTypeSelector", showBackground = true)
@Composable
private fun EventTypeSelectorPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SessionEventType.entries.forEach { selected ->
                EventTypeSelector(
                    selectedType = selected,
                    onTypeSelected = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
