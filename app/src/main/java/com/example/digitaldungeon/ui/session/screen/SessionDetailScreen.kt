package com.example.digitaldungeon.ui.session.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.session?.title ?: "Session") },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // Quick Note Input Section
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "Quick Note",
                        style = MaterialTheme.typography.titleMedium
                    )
                    EventTypeSelector(
                        selectedType = uiState.selectedEventType,
                        onTypeSelected = viewModel::onEventTypeSelected,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    QuickNoteInput(
                        text = uiState.quickNoteText,
                        onTextChange = viewModel::onQuickNoteTextChanged,
                        onAdd = viewModel::onAddQuickNote
                    )
                }

                // Timeline Section
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.events) { event ->
                        SessionEventCard(
                            event = event,
                            onEdit = { /* TODO: Implement edit dialog */ },
                            onDelete = { viewModel.onDeleteEvent(event.id) }
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
}
