package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.Campaign

@Composable
fun CampaignCard(
    campaign: Campaign,
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

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(campaign.name)

                Text(campaign.description)

            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onOpen
                ) {
                    Text("Open")
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