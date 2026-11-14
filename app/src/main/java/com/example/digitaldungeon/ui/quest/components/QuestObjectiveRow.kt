package com.example.digitaldungeon.ui.quest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One step of a quest, as a row of the objectives block.
 *
 * The number in front is the position in the list, not an id: a list of steps is read as "first
 * this, then that", and nothing else on the row carries that.
 *
 * The status chips sit under the text rather than beside it. Three chips and a title do not fit on
 * one line of a phone, and a status that wraps onto its own line anyway reads better with a label
 * above it than squeezed against the edge.
 *
 * @param position one-based number shown in front of the title.
 * @param canMoveUp whether the up arrow does anything; false for the first row.
 * @param canMoveDown whether the down arrow does anything; false for the last row.
 */
@Composable
fun QuestObjectiveRow(
    objective: QuestObjective,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onStatusSelected: (QuestObjectiveStatus) -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = "$position.",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = objective.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )

                // Both arrows stay on the row even when they do nothing: a button that
                // disappears at the edge makes the whole row jump as steps are reordered. The
                // disabled state and the content description say why it is dead - see the
                // `atEdge` descriptions.
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(
                            if (canMoveUp) {
                                R.string.quest_objective_move_up
                            } else {
                                R.string.quest_objective_move_up_at_edge
                            }
                        )
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (canMoveDown) {
                                R.string.quest_objective_move_down
                            } else {
                                R.string.quest_objective_move_down_at_edge
                            }
                        )
                    )
                }
            }

            // Only when set: an optional step is the exception, so saying nothing is how a
            // required one reads.
            if (objective.isOptional) {
                Text(
                    text = stringResource(R.string.quest_objective_optional_marker),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (objective.description.isNotBlank()) {
                Text(
                    text = objective.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            QuestObjectiveStatusSelector(
                selectedStatus = objective.status,
                onStatusSelected = onStatusSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
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

private val previewObjective = QuestObjective(
    id = 1,
    questId = 1,
    title = "Reach the Tser Pool before nightfall",
    description = "The road is open until dusk; after that the mists close in.",
    status = QuestObjectiveStatus.NOT_STARTED,
    orderIndex = 0,
    isOptional = false
)

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestObjectiveRowPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: the up arrow is dead. Middle row: both work. Last row: the down arrow
            // is dead - which is the whole reason the arrows are drawn disabled rather than hidden.
            QuestObjectiveRow(
                objective = previewObjective,
                position = 1,
                canMoveUp = false,
                canMoveDown = true,
                onStatusSelected = {},
                onEdit = {},
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {}
            )
            QuestObjectiveRow(
                objective = previewObjective.copy(
                    id = 2,
                    title = "Ask Ismark about the burgomaster",
                    description = "",
                    status = QuestObjectiveStatus.COMPLETED
                ),
                position = 2,
                canMoveUp = true,
                canMoveDown = true,
                onStatusSelected = {},
                onEdit = {},
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {}
            )
            QuestObjectiveRow(
                objective = previewObjective.copy(
                    id = 3,
                    title = "Recover the icon from the church",
                    description = "",
                    status = QuestObjectiveStatus.FAILED,
                    isOptional = true
                ),
                position = 3,
                canMoveUp = true,
                canMoveDown = false,
                onStatusSelected = {},
                onEdit = {},
                onMoveUp = {},
                onMoveDown = {},
                onDelete = {}
            )
        }
    }
}

/** A single objective: both arrows dead, because there is nothing to swap it with. */
@Preview(name = "The only objective", showBackground = true, widthDp = 400)
@Composable
private fun QuestObjectiveRowAlonePreview() {
    DigitalDungeonTheme {
        QuestObjectiveRow(
            objective = previewObjective,
            position = 1,
            canMoveUp = false,
            canMoveDown = false,
            onStatusSelected = {},
            onEdit = {},
            onMoveUp = {},
            onMoveDown = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
