package com.example.digitaldungeon.ui.session.screen

import androidx.compose.runtime.Immutable
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * The callbacks of [SessionDetailScreenContent], bundled by the part of the
 * screen they belong to.
 *
 * A stateless screen needs every one of its callbacks passed in, and this screen
 * has around twenty. Grouped the same way the campaign screen groups its backup
 * flow (`CampaignBackupActions`): one bundle per feature that is always wired as
 * a whole, so a call site reads as five things rather than twenty, and no-op
 * defaults let a preview or a test name only the bundle it cares about.
 *
 * Deliberately *not* one big bag of lambdas — the groups are the seams the screen
 * already has (writing a note, filtering, acting on an event, each dialog).
 */

/**
 * Writing the quick note at the top of the screen.
 *
 * There is no "can add" here: `QuickNoteInput` decides that from the text itself
 * (see [com.example.digitaldungeon.ui.session.components.isQuickNoteAddable]), so
 * [onAdd] is only ever called for text worth storing.
 */
@Immutable
data class QuickNoteActions(
    val onTypeSelected: (SessionEventType) -> Unit = {},
    val onTextChanged: (String) -> Unit = {},
    val onParticipantToggled: (EventParticipant) -> Unit = {},
    val onAdd: () -> Unit = {}
)

/**
 * Narrowing the timeline: the search field, the type chips, and the way back out
 * of both.
 *
 * @param onTypeFilterSelected `null` means "every type again".
 * @param onClearFilters drops the query and the type at once — the single way out
 *   offered by the "nothing matched" state.
 */
@Immutable
data class TimelineFilterActions(
    val onSearchQueryChange: (String) -> Unit = {},
    val onClearSearchQuery: () -> Unit = {},
    val onTypeFilterSelected: (SessionEventType?) -> Unit = {},
    val onClearFilters: () -> Unit = {}
)

/**
 * What a card on the timeline can ask for. Each takes the event it was pressed
 * on, because the card itself knows nothing about indexes or storage order.
 *
 * [onEdit] and [onDelete] are requests to *open* a dialog, not to change
 * anything: the writes happen through [EventEditDialogActions] and
 * [EventDeleteDialogActions] once the user has confirmed.
 */
@Immutable
data class SessionEventActions(
    val onEdit: (SessionEvent) -> Unit = {},
    val onDelete: (SessionEvent) -> Unit = {},
    val onMoveUp: (SessionEvent) -> Unit = {},
    val onMoveDown: (SessionEvent) -> Unit = {}
)

/**
 * The edit dialog's four fields and its two exits.
 *
 * @param onSave takes the id of the event being edited, which the dialog reads
 *   off [com.example.digitaldungeon.ui.session.state.SessionDetailUiState.editingEvent]
 *   — the only event it can possibly be saving.
 */
@Immutable
data class EventEditDialogActions(
    val onTitleChange: (String) -> Unit = {},
    val onTypeChange: (SessionEventType) -> Unit = {},
    val onContentChange: (String) -> Unit = {},
    val onParticipantToggled: (EventParticipant) -> Unit = {},
    val onSave: (Int) -> Unit = {},
    val onDismiss: () -> Unit = {}
)

/**
 * The delete confirmation. Which event is being deleted is already in the state,
 * so confirming needs no argument.
 */
@Immutable
data class EventDeleteDialogActions(
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)
