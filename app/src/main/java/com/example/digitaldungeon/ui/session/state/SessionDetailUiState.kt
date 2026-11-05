package com.example.digitaldungeon.ui.session.state

import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

data class SessionDetailUiState(
    val session: Session? = null,
    /** The complete timeline as stored. Filtering never shrinks this list. */
    val events: List<SessionEvent> = emptyList(),
    val quickNoteText: String = "",
    val selectedEventType: SessionEventType = SessionEventType.NOTE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val editingEvent: SessionEvent? = null,
    // Edit dialog fields
    val editTitle: String = "",
    val editType: SessionEventType = SessionEventType.NOTE,
    val editContent: String = "",
    // Delete confirmation dialog: the event the user is about to delete
    val eventPendingDeletion: SessionEvent? = null,
    // Timeline filtering (in memory, no extra query)
    val searchQuery: String = "",
    /** `null` means "every type", otherwise only events of that type are shown. */
    val typeFilter: SessionEventType? = null
) {
    /**
     * Single source of truth for "is the quick note worth adding".
     * The screen uses it to enable the Add button and the ViewModel uses it to
     * guard [com.example.digitaldungeon.ui.session.viewmodel.SessionDetailViewModel.onAddQuickNote],
     * so the rule lives in exactly one place.
     */
    val canAddQuickNote: Boolean
        get() = quickNoteText.isNotBlank()

    /** True while the timeline on screen is a subset of [events]. */
    val isFilterActive: Boolean
        get() = searchQuery.isNotBlank() || typeFilter != null

    /**
     * Derived view of [events]: case insensitive match on title or content plus
     * the optional type filter. Computed once per state instance instead of on
     * every read, so the screen can use it directly in a `LazyColumn`.
     */
    val visibleEvents: List<SessionEvent> =
        if (searchQuery.isBlank() && typeFilter == null) {
            events
        } else {
            val needle = searchQuery.trim()
            events.filter { event ->
                (typeFilter == null || event.type == typeFilter) && event.matches(needle)
            }
        }

    /** Nothing has ever been recorded — the "add your first note" case. */
    val isTimelineEmpty: Boolean
        get() = events.isEmpty()

    /** There are events, but none of them survived the filter. */
    val hasNoMatchingEvents: Boolean
        get() = events.isNotEmpty() && visibleEvents.isEmpty()

    /**
     * Reordering is only meaningful against the full timeline: "up" inside a
     * filtered list points at a neighbour the user cannot see, so the move
     * buttons are disabled (not silently ignored) while a filter is active.
     */
    val canReorder: Boolean
        get() = !isFilterActive
}

private fun SessionEvent.matches(needle: String): Boolean {
    if (needle.isEmpty()) return true

    return title?.contains(needle, ignoreCase = true) == true ||
        content.contains(needle, ignoreCase = true)
}
