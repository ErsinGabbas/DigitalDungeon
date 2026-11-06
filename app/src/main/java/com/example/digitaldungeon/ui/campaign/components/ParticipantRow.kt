package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One row of the editable participant collection on the campaign detail screen.
 */
@Composable
fun ParticipantRow(
    participant: EventParticipant,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.campaign_participant_summary,
                participant.name,
                participant.level,
                participant.maxHp
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(stringResource(R.string.common_edit))
            }
            Button(
                onClick = onRemove,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(stringResource(R.string.common_remove))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ParticipantRowPreview() {
    DigitalDungeonTheme {
        ParticipantRow(
            participant = EventParticipant(
                name = "Ireena Kolyana",
                level = 4,
                maxHp = 28
            ),
            onEdit = {},
            onRemove = {}
        )
    }
}
