package com.example.digitaldungeon.ui.quest.components

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
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.QuestSort
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The chip rows of the quest tracker, in one file because they are one idea with two shapes.
 *
 * A *filter* row has a "no filter" chip and lets the active chip be tapped off, so the selection
 * can be empty - that is what makes it a filter. A *selector* always has exactly one chip picked,
 * because the thing it stands for is never absent: a quest has a category whether the user chose
 * it or not. The same distinction the session UI draws between `EventTypeFilterRow` and
 * `EventTypeSelector`.
 *
 * Six small composables rather than one generic one: what varies between them is the enum, the
 * nullability of the selection and the wording of the "any" chip, and a signature carrying all
 * three would be harder to read than the rows themselves. What they do share - a horizontally
 * scrolling row with one spacing - is [QuestChipRow], so a row cannot drift out of alignment with
 * the others.
 */
@Composable
private fun QuestChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

/**
 * Status filter of the quest list. [selectedStatus] `null` shows every status.
 */
@Composable
fun QuestStatusFilterRow(
    selectedStatus: QuestStatus?,
    onStatusSelected: (QuestStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {

        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text(stringResource(R.string.quest_filter_status_any)) }
        )

        QuestStatus.entries.forEach { status ->
            val isSelected = status == selectedStatus
            FilterChip(
                selected = isSelected,
                // Tapping the active chip is the shortest way back to "any status"
                onClick = { onStatusSelected(if (isSelected) null else status) },
                label = { Text(status.label()) }
            )
        }
    }
}

/**
 * Category filter of the quest list. [selectedCategory] `null` shows every category.
 */
@Composable
fun QuestCategoryFilterRow(
    selectedCategory: QuestCategory?,
    onCategorySelected: (QuestCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {

        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text(stringResource(R.string.quest_filter_category_any)) }
        )

        QuestCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(if (isSelected) null else category) },
                label = { Text(category.label()) }
            )
        }
    }
}

/**
 * The order the list is read in. A selector and not a filter: the list always has an order, so
 * there is nothing to switch off here.
 */
@Composable
fun QuestSortRow(
    selectedSort: QuestSort,
    onSortSelected: (QuestSort) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestSort.entries.forEach { sort ->
            FilterChip(
                selected = sort == selectedSort,
                onClick = { onSortSelected(sort) },
                label = { Text(sort.label()) }
            )
        }
    }
}

/**
 * Moves a quest between statuses, on the quest screen and in the form.
 *
 * Every status is always offered, including the one the quest is already in and the ones it has
 * supposedly passed: nothing is terminal, so reopening a completed or failed quest is a tap and
 * not a special case. See `QuestStatus`.
 */
@Composable
fun QuestStatusSelector(
    selectedStatus: QuestStatus,
    onStatusSelected: (QuestStatus) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    QuestChipRow(modifier = modifier) {
        QuestStatus.entries.forEach { status ->
            FilterChip(
                selected = status == selectedStatus,
                onClick = { onStatusSelected(status) },
                enabled = enabled,
                label = { Text(status.label()) }
            )
        }
    }
}

/**
 * Moves one objective between its three states.
 *
 * A selector and not a checkbox: "done" and "failed" are different outcomes, and a step that
 * failed is not the same as one nobody has got to yet. Any direction is allowed here too - a step
 * ticked off by mistake has to be as easy to untick.
 */
@Composable
fun QuestObjectiveStatusSelector(
    selectedStatus: QuestObjectiveStatus,
    onStatusSelected: (QuestObjectiveStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestObjectiveStatus.entries.forEach { status ->
            FilterChip(
                selected = status == selectedStatus,
                onClick = { onStatusSelected(status) },
                label = { Text(status.label()) }
            )
        }
    }
}

@Composable
fun QuestRewardTypeSelector(
    selectedType: QuestRewardType,
    onTypeSelected: (QuestRewardType) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestRewardType.entries.forEach { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                label = { Text(type.label()) }
            )
        }
    }
}

@Composable
fun QuestConsequenceTypeSelector(
    selectedType: QuestConsequenceType,
    onTypeSelected: (QuestConsequenceType) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestConsequenceType.entries.forEach { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                label = { Text(type.label()) }
            )
        }
    }
}

@Composable
fun QuestCategorySelector(
    selectedCategory: QuestCategory,
    onCategorySelected: (QuestCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category.label()) }
            )
        }
    }
}

@Composable
fun QuestPrioritySelector(
    selectedPriority: QuestPriority,
    onPrioritySelected: (QuestPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    QuestChipRow(modifier = modifier) {
        QuestPriority.entries.forEach { priority ->
            FilterChip(
                selected = priority == selectedPriority,
                onClick = { onPrioritySelected(priority) },
                label = { Text(priority.label()) }
            )
        }
    }
}

@Preview(name = "Filter rows", showBackground = true, widthDp = 360)
@Composable
private fun QuestFilterRowsPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "Any" selected, then one real status - the two states of a filter row.
            QuestStatusFilterRow(
                selectedStatus = null,
                onStatusSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestStatusFilterRow(
                selectedStatus = QuestStatus.ACTIVE,
                onStatusSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestCategoryFilterRow(
                selectedCategory = null,
                onCategorySelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestCategoryFilterRow(
                selectedCategory = QuestCategory.MAIN,
                onCategorySelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestSortRow(
                selectedSort = QuestSort.PRIORITY,
                onSortSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Selectors", showBackground = true, widthDp = 360)
@Composable
private fun QuestSelectorsPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuestStatusSelector(
                selectedStatus = QuestStatus.COMPLETED,
                onStatusSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            // Disabled, which is how it looks while a status change is in flight.
            QuestStatusSelector(
                selectedStatus = QuestStatus.COMPLETED,
                onStatusSelected = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            QuestCategorySelector(
                selectedCategory = QuestCategory.FACTION,
                onCategorySelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestPrioritySelector(
                selectedPriority = QuestPriority.HIGH,
                onPrioritySelected = {},
                modifier = Modifier.fillMaxWidth()
            )
            QuestObjectiveStatusSelector(
                selectedStatus = QuestObjectiveStatus.COMPLETED,
                onStatusSelected = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
