package com.example.digitaldungeon.ui.quest.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.components.InlineErrorText
import com.example.digitaldungeon.ui.quest.state.QuestLinkKind
import com.example.digitaldungeon.ui.quest.state.QuestRelationsUiState
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The heading a group of links gets, read from the quest on screen.
 *
 * One `when` and the only place the two vocabularies meet: the stored row says `PARENT_CHILD`, the
 * reader wants "part of" or "leads to" depending on which end they are standing on.
 */
@StringRes
fun questLinkGroupLabelRes(kind: QuestLinkKind): Int =
    when (kind) {
        QuestLinkKind.CHILD_OF -> R.string.quest_relation_group_parents
        QuestLinkKind.PARENT_OF -> R.string.quest_relation_group_children
        QuestLinkKind.PRECEDES -> R.string.quest_relation_group_precedes
        QuestLinkKind.FOLLOWS -> R.string.quest_relation_group_follows
        QuestLinkKind.ALTERNATIVE -> R.string.quest_relation_group_alternatives
        QuestLinkKind.RELATED -> R.string.quest_relation_group_related
    }

/** The same six kinds as the sentence the picker offers: "leads to…", "is part of…". */
@StringRes
fun questLinkKindLabelRes(kind: QuestLinkKind): Int =
    when (kind) {
        QuestLinkKind.PARENT_OF -> R.string.quest_relation_kind_parent_of
        QuestLinkKind.CHILD_OF -> R.string.quest_relation_kind_child_of
        QuestLinkKind.PRECEDES -> R.string.quest_relation_kind_precedes
        QuestLinkKind.FOLLOWS -> R.string.quest_relation_kind_follows
        QuestLinkKind.ALTERNATIVE -> R.string.quest_relation_kind_alternative
        QuestLinkKind.RELATED -> R.string.quest_relation_kind_related
    }

/**
 * The links of a quest, grouped by what they mean here, with the form that adds one.
 *
 * Grouped and not flat, which is what the plan asks of this screen: a hierarchy read as a list -
 * what this quest is part of, what it leads to, what comes before and after it - rather than a
 * graph the user has to untangle. Each group's rows carry the title of the quest at the far end and
 * open it when tapped, so a story arc can be walked through without going back to the list.
 */
fun LazyListScope.relationsSection(
    relationsState: QuestRelationsUiState,
    actions: QuestRelationActions,
    onOpenQuest: (Int) -> Unit
) {

    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }

    item {
        Text(
            text = stringResource(R.string.quest_relations_heading),
            style = MaterialTheme.typography.titleSmall
        )
    }

    relationsState.error?.let { error ->
        item { InlineErrorText(error = error) }
    }

    if (relationsState.isLoading && relationsState.relations.isEmpty()) {
        item { CircularProgressIndicator() }
    }

    if (relationsState.hasNoLinks) {
        item {
            Text(
                text = stringResource(R.string.quest_relations_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Grouped in the order the kinds are declared in, which `links` has already sorted by.
    relationsState.links
        .groupBy { it.kind }
        .forEach { (kind, links) ->

            item {
                Text(
                    text = stringResource(questLinkGroupLabelRes(kind)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                count = links.size,
                key = { index -> "relation-${links[index].relation.id}" }
            ) { index ->

                val link = links[index]

                QuestLinkRow(
                    title = link.otherQuestTitle
                        ?: stringResource(R.string.quest_relation_unknown_quest),
                    onOpen = { onOpenQuest(link.otherQuestId) },
                    onRemove = { actions.onDeleteRequest(link) }
                )
            }
        }

    item {
        QuestRelationForm(
            state = relationsState,
            actions = actions,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * One linked quest: its title, a way in, and a way to drop the link.
 *
 * The whole row is the way in rather than a separate "open" button, which is how the quest cards on
 * the list behave; "Remove" is a text button because it is the rarer and more deliberate act.
 */
@Composable
private fun QuestLinkRow(
    title: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        TextButton(
            onClick = onOpen,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TextButton(onClick = onRemove) {
            Text(stringResource(R.string.common_remove))
        }
    }
}

/**
 * Picks a quest and what the link says about it.
 *
 * Two dropdowns rather than two chip rows: the campaign can hold dozens of quests, and a row of
 * chips one per quest would be unreadable long before that. The kind is a dropdown too, for
 * symmetry - six sentences do not fit on a phone's width either.
 *
 * Replaced wholesale by one sentence when the campaign has nothing else to link to: an empty picker
 * with a live button is the kind of dead end that reads as a bug.
 */
@Composable
private fun QuestRelationForm(
    state: QuestRelationsUiState,
    actions: QuestRelationActions,
    modifier: Modifier = Modifier
) {

    if (state.hasNoCandidates) {
        Text(
            text = stringResource(R.string.quest_relations_no_candidates),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = stringResource(R.string.quest_relation_kind_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        QuestPickerButton(
            label = stringResource(questLinkKindLabelRes(state.selectedKind)),
            options = QuestLinkKind.entries.map { kind ->
                stringResource(questLinkKindLabelRes(kind)) to { actions.onKindSelected(kind) }
            }
        )

        Text(
            text = stringResource(R.string.quest_relation_target_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        QuestPickerButton(
            label = state.selectedTarget?.title
                ?: stringResource(R.string.quest_relation_target_placeholder),
            options = state.candidates.map { quest ->
                quest.title to { actions.onTargetSelected(quest.id) }
            }
        )

        state.formError?.let { message ->
            Text(
                text = message.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = actions.onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.quest_relation_add))
        }
    }
}

/**
 * A button that opens a list and reports which line was tapped.
 *
 * The expanded flag is a local `remember` and not state the ViewModel carries, unlike every dialog
 * in this app: a menu that is open is not a question the user has answered, and a rotation closing
 * it loses nothing.
 *
 * Shared with the "came up in" block, which picks a session the same way this one picks a quest -
 * both lists are as long as the campaign is old, which is what ruled out chips for either.
 */
@Composable
internal fun QuestPickerButton(
    label: String,
    options: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (text, onPick) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onPick()
                        expanded = false
                    }
                )
            }
        }
    }
}

private val previewCandidates = listOf(
    Quest(
        id = 2,
        campaignId = 1,
        title = "Strahd's invitation",
        description = "",
        category = QuestCategory.MAIN,
        status = QuestStatus.NOT_STARTED,
        priority = QuestPriority.NORMAL,
        isHidden = false,
        createdAt = 1L
    ),
    Quest(
        id = 3,
        campaignId = 1,
        title = "The miller's missing daughter",
        description = "",
        category = QuestCategory.SIDE,
        status = QuestStatus.NOT_STARTED,
        priority = QuestPriority.NORMAL,
        isHidden = false,
        createdAt = 2L
    )
)

/**
 * The row and the form on their own, which is the part of the section that has layout worth
 * checking; the grouping is exercised by the quest screen's own previews.
 */
@Preview(showBackground = true, widthDp = 400)
@Composable
private fun QuestRelationFormPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            QuestLinkRow(
                title = "Strahd's invitation",
                onOpen = {},
                onRemove = {}
            )

            QuestRelationForm(
                state = QuestRelationsUiState(
                    questId = 1,
                    candidates = previewCandidates,
                    selectedTargetId = 2,
                    selectedKind = QuestLinkKind.CHILD_OF
                ),
                actions = QuestRelationActions()
            )
        }
    }
}

@Preview(name = "Refused: would close a cycle", showBackground = true, widthDp = 400)
@Composable
private fun QuestRelationFormRefusedPreview() {
    DigitalDungeonTheme {
        QuestRelationForm(
            state = QuestRelationsUiState(
                questId = 1,
                candidates = previewCandidates,
                selectedTargetId = 2,
                selectedKind = QuestLinkKind.PARENT_OF,
                formError = UiText.of(R.string.quest_relation_refused_cycle)
            ),
            actions = QuestRelationActions(),
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Nothing to link to: the form is replaced by the sentence that says why. */
@Preview(name = "No other quest in the campaign", showBackground = true, widthDp = 400)
@Composable
private fun QuestRelationFormNoCandidatesPreview() {
    DigitalDungeonTheme {
        QuestRelationForm(
            state = QuestRelationsUiState(questId = 1),
            actions = QuestRelationActions(),
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** A link row whose far end is not in the loaded list - the title falls back to a sentence. */
@Preview(name = "Unknown far end", showBackground = true, widthDp = 400)
@Composable
private fun QuestLinkRowUnknownPreview() {
    DigitalDungeonTheme {
        QuestLinkRow(
            title = stringResource(R.string.quest_relation_unknown_quest),
            onOpen = {},
            onRemove = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
