package com.example.digitaldungeon.ui.campaign.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * @param onEdit asks for the campaign's name and description to be edited - the
 * caller is expected to open the edit dialog.
 * @param onDelete asks for the campaign to be deleted - the caller is expected to
 * confirm first, deleting a campaign cascades to its sessions and their events.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(campaign.name, style = MaterialTheme.typography.titleLarge)
                Text(campaign.description, style = MaterialTheme.typography.bodyMedium)
                if (campaign.participants.isNotEmpty()) {
                    ParticipantsCard(participants = campaign.participants)
                }
            }
            // Stacked rather than side by side: three labelled buttons in a row
            // crowd the name out on a narrow screen.
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onOpen
                ) {
                    Text("Open")
                }

                Button(
                    onClick = onEdit
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

@Composable
private fun ParticipantsCard(participants: List<EventParticipant>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Participants",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            participants.forEach { participant ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Lvl ${participant.level} | HP: ${participant.maxHp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CampaignCardPreview() {
    DigitalDungeonTheme {
        CampaignCard(
            campaign = Campaign(
                id = 1,
                name = "Curse of Strahd",
                description = "Gothic horror in the valley of Barovia",
                createdAt = 0L,
                participants = listOf(
                    EventParticipant(name = "Ireena Kolyana", level = 4, maxHp = 28),
                    EventParticipant(name = "Ismark the Lesser", level = 3, maxHp = 24)
                )
            ),
            onEdit = {},
            onDelete = {},
            onOpen = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CampaignCardWithoutParticipantsPreview() {
    DigitalDungeonTheme {
        CampaignCard(
            campaign = Campaign(
                id = 2,
                name = "Tomb of Annihilation",
                description = "A jungle death march",
                createdAt = 0L
            ),
            onEdit = {},
            onDelete = {},
            onOpen = {}
        )
    }
}