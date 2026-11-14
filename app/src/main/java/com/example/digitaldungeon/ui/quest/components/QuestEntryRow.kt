package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One row of a titled, typed and optionally described thing: a reward or a consequence.
 *
 * Takes already resolved strings rather than a domain object, which is what lets the two blocks
 * share it: a reward and a consequence differ only in which enum names their kind and in the
 * wording around them, and neither difference reaches as far as the layout. Writing this twice
 * would mean keeping two identical cards in step for the rest of the project.
 *
 * No reorder arrows, unlike an objective row: rewards and consequences are sets, not sequences -
 * nothing is earned or happens before anything else - so there is no order to maintain.
 */
@Composable
fun QuestEntryRow(
    title: String,
    typeLabel: String,
    description: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.common_edit))
                }

                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestEntryRowPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestEntryRow(
                title = "300 gold from Ismark",
                typeLabel = stringResource(R.string.quest_reward_type_money),
                description = "Paid out of the burgomaster's estate.",
                onEdit = {},
                onDelete = {}
            )
            // The quiet case: a title and a kind, nothing else.
            QuestEntryRow(
                title = "The Vistani owe the party a favour",
                typeLabel = stringResource(R.string.quest_reward_type_reputation),
                description = "",
                onEdit = {},
                onDelete = {}
            )
            QuestEntryRow(
                title = "Ireena never reaches Vallaki",
                typeLabel = stringResource(R.string.quest_consequence_type_npc),
                description = "Strahd takes her at the Tser Pool.",
                onEdit = {},
                onDelete = {}
            )
        }
    }
}
