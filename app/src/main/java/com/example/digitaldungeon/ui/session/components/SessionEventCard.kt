package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.SessionEvent

@Composable
fun SessionEventCard(
    event: SessionEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.type) {
                com.example.digitaldungeon.domain.model.SessionEventType.SCENE -> MaterialTheme.colorScheme.primaryContainer
                com.example.digitaldungeon.domain.model.SessionEventType.COMBAT -> MaterialTheme.colorScheme.errorContainer
                com.example.digitaldungeon.domain.model.SessionEventType.DIALOGUE -> MaterialTheme.colorScheme.secondaryContainer
                com.example.digitaldungeon.domain.model.SessionEventType.DISCOVERY -> MaterialTheme.colorScheme.tertiaryContainer
                com.example.digitaldungeon.domain.model.SessionEventType.NOTE -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                event.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Text(
                text = event.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Edit")
                }
                Button(
                    onClick = onDelete
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
