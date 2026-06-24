package com.example.digitaldungeon.ui.session.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
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
    val uiState by viewModel.uiState.collectAsState()

    var editingEvent by remember { mutableStateOf<SessionEvent?>(null) }
    var editText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.session?.title ?: "Session")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
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
                        .padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.events,
                        key = { it.id }
                    ) { event ->
                        SessionEventCard(
                            event = event,
                            onEdit = {
                                editingEvent = event
                                editText = event.content
                            },
                            onDelete = {
                                viewModel.onDeleteEvent(event.id)
                            }
                        )
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // -------------------------
    // EDIT DIALOG
    // -------------------------
    if (editingEvent != null) {
        AlertDialog(
            onDismissRequest = {
                editingEvent = null
                editText = ""
            },
            title = { Text("Edit event") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Content") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onUpdateEvent(
                            eventId = editingEvent!!.id,
                            newContent = editText
                        )
                        editingEvent = null
                        editText = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingEvent = null
                        editText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickNoteSection(
    selectedType: SessionEventType,
    noteText: String,
    onTypeSelected: (SessionEventType) -> Unit,
    onTextChanged: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
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
            onAdd = onAdd
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
        Text(
            text = "No events yet",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}