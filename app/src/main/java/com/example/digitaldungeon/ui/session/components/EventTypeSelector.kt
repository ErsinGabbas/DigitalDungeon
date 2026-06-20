package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.SessionEventType

@Composable
fun EventTypeSelector(
    selectedType: SessionEventType,
    onTypeSelected: (SessionEventType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        SessionEventType.values().forEach { type ->
            Button(
                onClick = { onTypeSelected(type) },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(type.name)
            }
        }
    }
}
