package com.example.digitaldungeon.ui.session.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.components.DeleteEventConfirmDialog
import com.example.digitaldungeon.ui.session.components.EventTypeSelector
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

            if (uiState.events.isEmpty()) {
                EmptyTimelineState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.events, key = { it.id }) { event ->
                        SessionEventCard(
                            event = event,
                            onEdit = {
                                viewModel.onEditEvent(event)
                            },
                            onDelete = {
                                viewModel.onDeleteEventClick(event)
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

@Composable
private fun EmptyTimelineState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "No events yet",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pick a type above and add your first note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
