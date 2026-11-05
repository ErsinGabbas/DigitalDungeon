package com.example.digitaldungeon.ui.session.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.components.DeleteEventConfirmDialog
import com.example.digitaldungeon.ui.session.components.EmptyTimelineState
import com.example.digitaldungeon.ui.session.components.EventSearchBar
import com.example.digitaldungeon.ui.session.components.EventTypeFilterRow
import com.example.digitaldungeon.ui.session.components.EventTypeSelector
import com.example.digitaldungeon.ui.session.components.NoMatchingEventsState
import com.example.digitaldungeon.ui.session.components.QuickNoteInput
import com.example.digitaldungeon.ui.session.components.SessionEventCard
import com.example.digitaldungeon.ui.session.viewmodel.SessionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val editingEvent = uiState.editingEvent
    val eventPendingDeletion = uiState.eventPendingDeletion

    val snackbarHostState = remember { SnackbarHostState() }

    // Only the screen talks to Android: the ViewModel hands over a plain string
    val context = LocalContext.current

    // A line of red text at the bottom of the column is easy to miss while the
    // timeline is scrolled, so errors are surfaced as a snackbar instead
    val error = uiState.error
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.title ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val markdown = viewModel.buildSessionMarkdown()
                            if (markdown != null) {
                                context.shareAsPlainText(
                                    subject = uiState.session?.title.orEmpty(),
                                    body = markdown
                                )
                            }
                        },
                        // Nothing to export before the session is loaded
                        enabled = uiState.session != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            // Says "full" on purpose: the export ignores the
                            // filter and always covers the whole log
                            contentDescription = "Share full session log"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (uiState.isLoading) {
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
                selectedType = uiState.selectedEventType,
                noteText = uiState.quickNoteText,
                canAdd = uiState.canAddQuickNote,
                onTypeSelected = viewModel::onEventTypeSelected,
                onTextChanged = viewModel::onQuickNoteTextChanged,
                onAdd = viewModel::onAddQuickNote
            )

            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            EventSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClearQuery = viewModel::onClearSearchQuery
            )

            EventTypeFilterRow(
                selectedType = uiState.typeFilter,
                onTypeSelected = viewModel::onTypeFilterSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (uiState.isFilterActive) {
                FilterStatusLine(
                    visibleCount = uiState.visibleEvents.size,
                    totalCount = uiState.events.size
                )
            }

            when {
                uiState.isTimelineEmpty -> EmptyTimelineState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                uiState.hasNoMatchingEvents -> NoMatchingEventsState(
                    onClearFilters = viewModel::onClearFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.visibleEvents,
                        key = { _, event -> event.id }
                    ) { index, event ->
                        SessionEventCard(
                            event = event,
                            onEdit = {
                                viewModel.onEditEvent(event)
                            },
                            onDelete = {
                                viewModel.onDeleteEventClick(event)
                            },
                            // The screen owns the position; the card only gets
                            // the verdict. Edges are disabled so the arrows
                            // never look broken, and the whole pair is off
                            // while filtering, where "the event above" on
                            // screen is not the event above in storage.
                            canMoveUp = uiState.canReorder && index > 0,
                            canMoveDown = uiState.canReorder &&
                                index < uiState.visibleEvents.lastIndex,
                            onMoveUp = {
                                viewModel.onMoveEventUp(event)
                            },
                            onMoveDown = {
                                viewModel.onMoveEventDown(event)
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------
    // DIALOGS (VM DRIVEN)
    //
    // One branch, so edit and delete confirmation can never be shown at once —
    // the ViewModel also clears the other whenever one is opened.
    // -------------------------
    when {
        editingEvent != null -> EditEventDialog(
            title = uiState.editTitle,
            type = uiState.editType,
            content = uiState.editContent,
            onTitleChange = viewModel::onEditTitleChange,
            onTypeChange = viewModel::onEditTypeChange,
            onContentChange = viewModel::onEditContentChange,
            onSave = { viewModel.onUpdateEvent(eventId = editingEvent.id) },
            onDismiss = viewModel::onEditDialogDismiss
        )

        eventPendingDeletion != null -> DeleteEventConfirmDialog(
            event = eventPendingDeletion,
            onConfirm = viewModel::onConfirmDeleteEvent,
            onDismiss = viewModel::onDeleteDialogDismiss
        )
    }
}

/**
 * Explains, in one line, both surprises of a filtered timeline: the arrows are
 * greyed out, and the share action still exports everything.
 */
@Composable
private fun FilterStatusLine(
    visibleCount: Int,
    totalCount: Int
) {
    Text(
        text = "Showing $visibleCount of $totalCount events. " +
            "Reordering is off while filtering; sharing still exports the full log.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * Hands the already formatted log to whatever app the user picks. No file and
 * no FileProvider: plain text goes through the share sheet as an extra.
 */
private fun Context.shareAsPlainText(subject: String, body: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    val chooser = Intent.createChooser(sendIntent, "Share session log")

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
    onTitleChange: (String) -> Unit,
    onTypeChange: (SessionEventType) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit event") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Event Type",
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
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QuickNoteSection(
    selectedType: SessionEventType,
    noteText: String,
    canAdd: Boolean,
    onTypeSelected: (SessionEventType) -> Unit,
    onTextChanged: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "Quick Note",
            style = MaterialTheme.typography.titleMedium
        )

        EventTypeSelector(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        QuickNoteInput(
            text = noteText,
            onTextChange = onTextChanged,
            onAdd = onAdd,
            canAdd = canAdd
        )
    }
}
