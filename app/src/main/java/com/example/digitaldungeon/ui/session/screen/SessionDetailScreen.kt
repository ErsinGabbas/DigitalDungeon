package com.example.digitaldungeon.ui.session.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.components.DeleteEventConfirmDialog
import com.example.digitaldungeon.ui.session.components.EmptyTimelineState
import com.example.digitaldungeon.ui.session.components.EventMoveAvailability
import com.example.digitaldungeon.ui.session.components.EventParticipantPicker
import com.example.digitaldungeon.ui.session.components.EventSearchBar
import com.example.digitaldungeon.ui.session.components.EventTypeFilterRow
import com.example.digitaldungeon.ui.session.components.EventTypeSelector
import com.example.digitaldungeon.ui.session.components.FilterStatusLine
import com.example.digitaldungeon.ui.session.components.NoMatchingEventsState
import com.example.digitaldungeon.ui.session.components.QuickNoteInput
import com.example.digitaldungeon.ui.session.components.SessionEventCard
import com.example.digitaldungeon.ui.session.components.previewAllEvents
import com.example.digitaldungeon.ui.session.components.previewCampaignParticipants
import com.example.digitaldungeon.ui.session.components.previewDepartedParticipant
import com.example.digitaldungeon.ui.session.components.previewNow
import com.example.digitaldungeon.ui.session.components.previewSession
import com.example.digitaldungeon.ui.session.components.sessionMarkdownLabels
import com.example.digitaldungeon.ui.session.state.SessionDetailUiState
import com.example.digitaldungeon.ui.session.viewmodel.SessionDetailViewModel
import com.example.digitaldungeon.ui.text.asString
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The session screen with its ViewModel attached: collects the state, turns the
 * ViewModel's methods into the callback bundles [SessionDetailScreenContent]
 * expects, and owns the two things that need Android rather than state — the
 * share sheet and the error snackbar.
 *
 * Everything that can be looked at without Hilt lives in the content function
 * instead, in the same split as `CampaignScreen` / `CampaignScreenContent`.
 *
 * The signature is fixed: `AppNavigation` calls exactly this.
 */
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // One reading of the clock for the whole timeline, so every card decides
    // "today or earlier" against the same moment. Keyed on the event list, which
    // means it is taken again whenever the timeline changes — enough to keep a
    // session running across midnight honest, without a ticking recomposition.
    val now = remember(uiState.events) { System.currentTimeMillis() }

    // Only the screen talks to Android: the ViewModel hands over a plain string
    val context = LocalContext.current

    // Everything the exported document says, read here and handed to the ViewModel,
    // so the pure formatter never needs a Context of its own
    val markdownLabels = sessionMarkdownLabels()
    val shareChooserTitle = stringResource(R.string.session_share_chooser_title)

    // A line of red text at the bottom of the column is easy to miss while the
    // timeline is scrolled, so errors are surfaced as a snackbar instead.
    // Resolved in composition, not inside the effect: asString() is a @Composable
    // read, and doing it here is also what makes the message follow a language change.
    val error = uiState.error
    val errorMessage = error?.asString()
    LaunchedEffect(error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // Remembered per ViewModel: these are bound method references, so rebuilding
    // them on every recomposition would hand the content equal-but-new bundles
    val quickNoteActions = remember(viewModel) {
        QuickNoteActions(
            onTypeSelected = viewModel::onEventTypeSelected,
            onTextChanged = viewModel::onQuickNoteTextChanged,
            onParticipantToggled = viewModel::onQuickNoteParticipantToggled,
            onAdd = viewModel::onAddQuickNote
        )
    }

    val filterActions = remember(viewModel) {
        TimelineFilterActions(
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onClearSearchQuery = viewModel::onClearSearchQuery,
            onTypeFilterSelected = viewModel::onTypeFilterSelected,
            onClearFilters = viewModel::onClearFilters
        )
    }

    val eventActions = remember(viewModel) {
        SessionEventActions(
            onEdit = viewModel::onEditEvent,
            onDelete = viewModel::onDeleteEventClick,
            onMoveUp = viewModel::onMoveEventUp,
            onMoveDown = viewModel::onMoveEventDown
        )
    }

    val editDialogActions = remember(viewModel) {
        EventEditDialogActions(
            onTitleChange = viewModel::onEditTitleChange,
            onTypeChange = viewModel::onEditTypeChange,
            onContentChange = viewModel::onEditContentChange,
            onParticipantToggled = viewModel::onEditParticipantToggled,
            onSave = viewModel::onUpdateEvent,
            onDismiss = viewModel::onEditDialogDismiss
        )
    }

    val deleteDialogActions = remember(viewModel) {
        EventDeleteDialogActions(
            onConfirm = viewModel::onConfirmDeleteEvent,
            onDismiss = viewModel::onDeleteDialogDismiss
        )
    }

    SessionDetailScreenContent(
        state = uiState,
        now = now,
        onBack = onBack,
        // Not remembered on purpose: it reads the session title out of the state
        // at the moment it is pressed, and a cached lambda could share a stale one
        onShareSessionLog = {
            val markdown = viewModel.buildSessionMarkdown(markdownLabels)
            if (markdown != null) {
                context.shareAsPlainText(
                    subject = uiState.session?.title.orEmpty(),
                    body = markdown,
                    chooserTitle = shareChooserTitle
                )
            }
        },
        quickNoteActions = quickNoteActions,
        filterActions = filterActions,
        eventActions = eventActions,
        editDialogActions = editDialogActions,
        deleteDialogActions = deleteDialogActions,
        snackbarHostState = snackbarHostState
    )
}

/**
 * The session screen without its ViewModel, so it can be previewed and tested.
 *
 * This is where the wiring lives, and the wiring is what breaks silently during a
 * refactor: that `canReorder` reaches the right arrow of the right card, that
 * `isTimelineEmpty` shows "no events yet" while `hasNoMatchingEvents` shows
 * "nothing matched", that one dialog at most is ever on screen. None of it is
 * visible to a ViewModel test, and `hiltViewModel()` would make it untestable.
 *
 * Deliberately free of the share sheet and the error snackbar effect: both need
 * an Android `Context` or a `LaunchedEffect` over the ViewModel, and both belong
 * to [SessionDetailScreen]. What arrives here is a callback and a state.
 *
 * @param now the moment every timestamp on the timeline is read against; see
 *   [com.example.digitaldungeon.ui.session.components.formatEventTimestamp].
 *   Passed in rather than read from the clock, so a preview and a test render
 *   the same stamps on every run.
 * @param onShareSessionLog the share action in the app bar. Disabled until a
 *   session is loaded, and it always exports the full log, filter or not.
 * @param snackbarHostState hosted here, driven from [SessionDetailScreen]. A
 *   caller that does not care gets its own, which simply never shows anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreenContent(
    state: SessionDetailUiState,
    now: Long,
    onBack: () -> Unit,
    onShareSessionLog: () -> Unit,
    quickNoteActions: QuickNoteActions = QuickNoteActions(),
    filterActions: TimelineFilterActions = TimelineFilterActions(),
    eventActions: SessionEventActions = SessionEventActions(),
    editDialogActions: EventEditDialogActions = EventEditDialogActions(),
    deleteDialogActions: EventDeleteDialogActions = EventDeleteDialogActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val editingEvent = state.editingEvent
    val eventPendingDeletion = state.eventPendingDeletion

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.session?.title
                            ?: stringResource(R.string.session_detail_title_fallback)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.session_action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onShareSessionLog,
                        // Nothing to export before the session is loaded
                        enabled = state.session != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            // Says "full" on purpose: the export ignores the
                            // filter and always covers the whole log
                            contentDescription =
                                stringResource(R.string.session_action_share_full_log)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            QuickNoteSection(
                selectedType = state.selectedEventType,
                noteText = state.quickNoteText,
                // Empty for a campaign without participants, and the picker
                // then draws nothing at all
                participantOptions = state.campaignParticipants,
                selectedParticipants = state.quickNoteParticipants,
                actions = quickNoteActions
            )

            Text(
                text = stringResource(R.string.session_timeline_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            EventSearchBar(
                query = state.searchQuery,
                onQueryChange = filterActions.onSearchQueryChange,
                onClearQuery = filterActions.onClearSearchQuery
            )

            EventTypeFilterRow(
                selectedType = state.typeFilter,
                onTypeSelected = filterActions.onTypeFilterSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (state.isFilterActive) {
                FilterStatusLine(
                    visibleCount = state.visibleEvents.size,
                    totalCount = state.events.size
                )
            }

            when {
                // Two different states with two different ways out: nothing has
                // ever been written here, versus everything written is being
                // hidden by the filter
                state.isTimelineEmpty -> EmptyTimelineState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                state.hasNoMatchingEvents -> NoMatchingEventsState(
                    onClearFilters = filterActions.onClearFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = state.visibleEvents,
                        key = { _, event -> event.id }
                    ) { index, event ->
                        SessionEventCard(
                            event = event,
                            now = now,
                            onEdit = {
                                eventActions.onEdit(event)
                            },
                            onDelete = {
                                eventActions.onDelete(event)
                            },
                            // The screen owns the position, the card gets the
                            // verdict *and the reason for it*: either there is
                            // no neighbour in that direction, or a filter is on
                            // and "the event above" on screen is not the event
                            // above in storage. The card says which one.
                            moveUp = EventMoveAvailability.of(
                                reorderingEnabled = state.canReorder,
                                isAtEdge = index == 0
                            ),
                            moveDown = EventMoveAvailability.of(
                                reorderingEnabled = state.canReorder,
                                isAtEdge = index == state.visibleEvents.lastIndex
                            ),
                            onMoveUp = {
                                eventActions.onMoveUp(event)
                            },
                            onMoveDown = {
                                eventActions.onMoveDown(event)
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------
    // DIALOGS (STATE DRIVEN)
    //
    // One branch, so edit and delete confirmation can never be shown at once —
    // the ViewModel also clears the other whenever one is opened.
    // -------------------------
    when {
        editingEvent != null -> EditEventDialog(
            title = state.editTitle,
            type = state.editType,
            content = state.editContent,
            // The roster plus anyone this event names who has since left it, so
            // an old cast is visible instead of silently carried along
            participantOptions = state.editParticipantOptions,
            selectedParticipants = state.editParticipants,
            onTitleChange = editDialogActions.onTitleChange,
            onTypeChange = editDialogActions.onTypeChange,
            onContentChange = editDialogActions.onContentChange,
            onParticipantToggle = editDialogActions.onParticipantToggled,
            onSave = { editDialogActions.onSave(editingEvent.id) },
            onDismiss = editDialogActions.onDismiss
        )

        eventPendingDeletion != null -> DeleteEventConfirmDialog(
            event = eventPendingDeletion,
            onConfirm = deleteDialogActions.onConfirm,
            onDismiss = deleteDialogActions.onDismiss
        )
    }
}

/**
 * Hands the already formatted log to whatever app the user picks. No file and
 * no FileProvider: plain text goes through the share sheet as an extra.
 */
private fun Context.shareAsPlainText(
    subject: String,
    body: String,
    chooserTitle: String
) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    // Passed in rather than read from this Context: the caller is a composable and
    // already resolves its strings the same way as everything else on screen
    val chooser = Intent.createChooser(sendIntent, chooserTitle)

    val activity = findActivity()
    if (activity != null) {
        activity.startActivity(chooser)
    } else {
        // Starting an activity from a non-activity context requires its own task
        startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * `LocalContext.current` is normally the hosting activity, but it can be a
 * wrapper around it (previews, embedded contexts), so unwrap before launching.
 */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun EditEventDialog(
    title: String,
    type: SessionEventType,
    content: String,
    participantOptions: List<EventParticipant>,
    selectedParticipants: List<EventParticipant>,
    onTitleChange: (String) -> Unit,
    onTypeChange: (SessionEventType) -> Unit,
    onContentChange: (String) -> Unit,
    onParticipantToggle: (EventParticipant) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_edit_title)) },
        text = {
            Column(
                // The participant chips made this content taller than a dialog
                // on a small screen; scrolling keeps the Content field reachable.
                // A plain scroll, not a lazy list — the lists here are tiny and a
                // lazy child inside a dialog cannot measure itself.
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.event_field_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.event_field_type_label),
                    style = MaterialTheme.typography.labelSmall
                )
                EventTypeSelector(
                    selectedType = type,
                    onTypeSelected = onTypeChange,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    label = { Text(stringResource(R.string.event_field_content_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                EventParticipantPicker(
                    options = participantOptions,
                    selected = selectedParticipants,
                    onToggle = onParticipantToggle
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.session_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.session_action_cancel))
            }
        }
    )
}

/**
 * Type, cast and text for the note being written.
 *
 * No "can add" flag passes through here: [QuickNoteInput] decides that from the
 * text it is given, which is the only way the rule cannot be contradicted on the
 * way down.
 */
@Composable
private fun QuickNoteSection(
    selectedType: SessionEventType,
    noteText: String,
    participantOptions: List<EventParticipant>,
    selectedParticipants: List<EventParticipant>,
    actions: QuickNoteActions
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.session_quick_note_heading),
            style = MaterialTheme.typography.titleMedium
        )

        EventTypeSelector(
            selectedType = selectedType,
            onTypeSelected = actions.onTypeSelected,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Above the text field and the Add button, so the cast is chosen before
        // the note is committed. Draws nothing while the campaign has no
        // participants, which is also the state before the session has loaded.
        EventParticipantPicker(
            options = participantOptions,
            selected = selectedParticipants,
            onToggle = actions.onParticipantToggled,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        QuickNoteInput(
            text = noteText,
            onTextChange = actions.onTextChanged,
            onAdd = actions.onAdd
        )
    }
}

/** A loaded session with a timeline on it, nothing filtered. */
private val previewLoadedState = SessionDetailUiState(
    session = previewSession,
    events = previewAllEvents,
    campaignParticipants = previewCampaignParticipants,
    quickNoteText = "The captain surrenders",
    selectedEventType = SessionEventType.COMBAT,
    quickNoteParticipants = previewCampaignParticipants.take(1)
)

@Preview(name = "SessionDetailScreenContent", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = previewLoadedState,
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/**
 * A filtered timeline: the status line appears, and every arrow on every card is
 * disabled with the filter as its stated reason — reachable on the card itself,
 * which is what the status line cannot be once the list is scrolled.
 */
@Preview(name = "Filtered timeline", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentFilteredPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = previewLoadedState.copy(
                quickNoteText = "",
                quickNoteParticipants = emptyList(),
                typeFilter = SessionEventType.DIALOGUE
            ),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/** Nothing has ever been recorded: the "add your first note" state. */
@Preview(name = "Empty timeline", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentEmptyPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = SessionDetailUiState(
                session = previewSession,
                campaignParticipants = previewCampaignParticipants
            ),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/**
 * There are events, the filter is hiding all of them. Worded and acted on
 * differently from the empty timeline above — that is the pair that must never
 * be swapped.
 */
@Preview(name = "No matching events", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentNoMatchesPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = previewLoadedState.copy(
                quickNoteText = "",
                quickNoteParticipants = emptyList(),
                searchQuery = "dragonlance"
            ),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/** Before anything has loaded: no title in the bar, no share, no timeline. */
@Preview(name = "Loading", showBackground = true, heightDp = 400)
@Composable
private fun SessionDetailScreenContentLoadingPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = SessionDetailUiState(isLoading = true),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/**
 * The edit dialog over the screen, on an event whose cast includes someone the
 * campaign has dropped: the option is still offered and still checked, so saving
 * an untouched dialog keeps the snapshot as recorded.
 */
@Preview(name = "Edit dialog open", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentEditingPreview() {
    val edited = previewAllEvents[2]

    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = previewLoadedState.copy(
                editingEvent = edited,
                editTitle = edited.title.orEmpty(),
                editType = edited.type,
                editContent = edited.content,
                editParticipants = listOf(
                    previewCampaignParticipants.first(),
                    previewDepartedParticipant
                )
            ),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}

/** The delete confirmation. Mutually exclusive with the edit dialog above. */
@Preview(name = "Delete dialog open", showBackground = true, heightDp = 900)
@Composable
private fun SessionDetailScreenContentDeletingPreview() {
    DigitalDungeonTheme {
        SessionDetailScreenContent(
            state = previewLoadedState.copy(
                eventPendingDeletion = previewAllEvents.first()
            ),
            now = previewNow,
            onBack = {},
            onShareSessionLog = {}
        )
    }
}
