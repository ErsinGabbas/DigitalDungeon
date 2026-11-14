package com.example.digitaldungeon.ui.quest.components

import androidx.compose.runtime.Immutable
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.QuestLink
import com.example.digitaldungeon.ui.quest.state.QuestLinkKind
import com.example.digitaldungeon.ui.quest.state.QuestSessionEntry
import com.example.digitaldungeon.ui.quest.state.QuestSort

/**
 * Everything the quest list can ask of its ViewModel, bundled for the same reason the campaign and
 * session screens bundle theirs: one feature, one wiring, and no-op defaults so a preview or a
 * draw-only test need not repeat nine lambdas.
 *
 * Opening a quest and starting a new one are *not* in here. Both are navigation, they are the only
 * callbacks that leave the screen, and the screen receives them from the navigation graph.
 *
 * @param onSearchQueryChange the search field was typed into.
 * @param onClearSearchQuery the search field was emptied by its own button.
 * @param onStatusSelected a status filter chip was tapped; `null` means "any status".
 * @param onCategorySelected a category filter chip was tapped; `null` means "any category".
 * @param onSortSelected the list order was changed.
 * @param onClearFilters the search and both filters should go back to showing everything.
 * @param onDeleteRequest this quest should be confirmed for deletion.
 * @param onConfirmDelete the deletion was confirmed.
 * @param onCancelDelete the confirmation was declined.
 */
@Immutable
data class QuestListActions(
    val onSearchQueryChange: (String) -> Unit = {},
    val onClearSearchQuery: () -> Unit = {},
    val onStatusSelected: (QuestStatus?) -> Unit = {},
    val onCategorySelected: (QuestCategory?) -> Unit = {},
    val onSortSelected: (QuestSort) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onDeleteRequest: (Quest) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * What one quest's screen can ask for.
 *
 * Only the status, because that is the one thing the screen changes in place; everything else about
 * a quest is changed through the form. Navigating back and opening the form are navigation and come
 * from the graph.
 *
 * @param onStatusSelected a status chip was tapped. Any status is reachable from any other,
 *   reopening a finished quest included - see `QuestStatus`.
 */
@Immutable
data class QuestDetailActions(
    val onStatusSelected: (QuestStatus) -> Unit = {}
)

/**
 * Everything the objectives block of the quest screen can ask for: the add / edit form, the
 * per-row gestures and the delete confirmation.
 *
 * A bundle of its own rather than more fields on [QuestDetailActions], the same way the campaign
 * screen keeps its participant and session actions apart: they are two features on one screen, and
 * a preview that only cares about the quest should not have to spell out eleven lambdas.
 *
 * @param onTitleChange the form's objective field was typed into.
 * @param onDescriptionChange the form's details field was typed into.
 * @param onOptionalChange the "optional step" switch was flipped.
 * @param onAdd "Add objective" - append what the form holds.
 * @param onUpdate "Save objective" - rewrite the one being edited. May be refused, in which case
 *   the form stays put carrying
 *   [com.example.digitaldungeon.ui.quest.state.QuestObjectivesUiState.formError].
 * @param onEditRequest this objective should be loaded into the form.
 * @param onCancelEdit the form should go back to adding.
 * @param onStatusSelected a status chip on this objective was tapped.
 * @param onMoveUp / @param onMoveDown reorder gestures; the screen disables them at the edges.
 * @param onDeleteRequest this objective should be confirmed for deletion.
 * @param onConfirmDelete the deletion was confirmed.
 * @param onCancelDelete the confirmation was declined.
 */
@Immutable
data class QuestObjectiveActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onOptionalChange: (Boolean) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onUpdate: () -> Unit = {},
    val onEditRequest: (QuestObjective) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onStatusSelected: (QuestObjective, QuestObjectiveStatus) -> Unit = { _, _ -> },
    val onMoveUp: (QuestObjective) -> Unit = {},
    val onMoveDown: (QuestObjective) -> Unit = {},
    val onDeleteRequest: (QuestObjective) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * Everything the rewards block can ask for: its add / edit form, the per-row buttons and the
 * delete confirmation.
 *
 * The same shape as [QuestConsequenceActions] and, like the two state classes behind them, kept as
 * two separate types rather than one generic: they carry different type enums, and a generic would
 * push a type argument onto every call site to save a dozen lines here.
 *
 * @param onTypeSelected a kind chip in the form was tapped.
 * @param onUpdate may be refused, in which case the form stays put carrying
 *   [com.example.digitaldungeon.ui.quest.state.QuestRewardsUiState.formError].
 */
@Immutable
data class QuestRewardActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onTypeSelected: (QuestRewardType) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onUpdate: () -> Unit = {},
    val onEditRequest: (QuestReward) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onDeleteRequest: (QuestReward) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/** Everything the consequences block can ask for; see [QuestRewardActions] for the shape. */
@Immutable
data class QuestConsequenceActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onTypeSelected: (QuestConsequenceType) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onUpdate: () -> Unit = {},
    val onEditRequest: (QuestConsequence) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onDeleteRequest: (QuestConsequence) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * Everything the notes block can ask for.
 *
 * Shorter than the two above because a note has one field: no title to change separately from a
 * body, and no kind to pick.
 */
@Immutable
data class QuestNoteActions(
    val onContentChange: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onUpdate: () -> Unit = {},
    val onEditRequest: (QuestNote) -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onDeleteRequest: (QuestNote) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * Everything the links block can ask for.
 *
 * Shorter than the blocks above because a link has no text to type and nothing to edit: it is two
 * quests and a kind, so it is either added or removed. Changing one means removing it and adding
 * another, which is also why there is no `onUpdate` here.
 *
 * Opening a linked quest is *not* in here. It is navigation, and the screen receives it from the
 * graph like every other move between screens.
 *
 * @param onKindSelected the picker chose what the link says about this quest.
 * @param onTargetSelected the picker chose the quest at the far end, by id.
 * @param onAdd may be refused - the two quests may be in different campaigns, the link may already
 *   exist, or it would make the quest its own ancestor - in which case the reason lands in
 *   [com.example.digitaldungeon.ui.quest.state.QuestRelationsUiState.formError].
 * @param onDeleteRequest this link should be confirmed for removal. Takes the whole link, because
 *   the question has to name the quest at the other end.
 */
@Immutable
data class QuestRelationActions(
    val onKindSelected: (QuestLinkKind) -> Unit = {},
    val onTargetSelected: (Int) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onDeleteRequest: (QuestLink) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * Everything the "came up in" block can ask for.
 *
 * Opening a session is *not* in here: it is navigation, and the screen gets it from the graph like
 * every other move between screens. There is no edit either - a record of a quest coming up in a
 * session has no field to change.
 *
 * @param onSessionSelected the picker chose a session, by id.
 * @param onAdd may be refused - already recorded, or a session of another campaign - in which case
 *   the reason lands in
 *   [com.example.digitaldungeon.ui.quest.state.QuestSessionsUiState.formError].
 * @param onDeleteRequest this record should be confirmed for removal. Takes the whole entry,
 *   because the question has to name the session.
 */
@Immutable
data class QuestSessionActions(
    val onSessionSelected: (Int) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onDeleteRequest: (QuestSessionEntry) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {}
)

/**
 * What the create / edit form can ask for.
 *
 * @param onTitleChange the title field was typed into.
 * @param onDescriptionChange the description field was typed into.
 * @param onCategorySelected a category chip was tapped.
 * @param onStatusSelected a status chip was tapped.
 * @param onPrioritySelected a priority chip was tapped.
 * @param onHiddenChange the "the party does not know yet" switch was flipped.
 * @param onInGameDateChange the in-world date field was typed into.
 * @param onToggleOptionalFields the second half of the form was folded or unfolded.
 * @param onSave the save button. May be refused, in which case the form stays on screen carrying
 *   [com.example.digitaldungeon.ui.quest.state.QuestEditUiState.titleError].
 */
@Immutable
data class QuestEditActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onCategorySelected: (QuestCategory) -> Unit = {},
    val onStatusSelected: (QuestStatus) -> Unit = {},
    val onPrioritySelected: (QuestPriority) -> Unit = {},
    val onHiddenChange: (Boolean) -> Unit = {},
    val onInGameDateChange: (String) -> Unit = {},
    val onToggleOptionalFields: () -> Unit = {},
    val onSave: () -> Unit = {}
)
