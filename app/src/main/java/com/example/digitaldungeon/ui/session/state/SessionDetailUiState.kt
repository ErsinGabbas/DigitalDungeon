package com.example.digitaldungeon.ui.session.state

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.components.isQuickNoteAddable
import com.example.digitaldungeon.ui.text.UiText

data class SessionDetailUiState(
    val session: Session? = null,
    /** The complete timeline as stored. Filtering never shrinks this list. */
    val events: List<SessionEvent> = emptyList(),
    /**
     * Who the campaign of this session currently lists, i.e. what the pickers
     * offer.
     *
     * Empty until the session has loaded: the campaign is only reachable through
     * `session.campaignId`, so this arrives one step after the session does.
     * It is *not* where an event's cast is read from — events carry their own
     * copy, see [SessionEvent.participants].
     */
    val campaignParticipants: List<EventParticipant> = emptyList(),
    val quickNoteText: String = "",
    val selectedEventType: SessionEventType = SessionEventType.NOTE,
    /**
     * Cast picked for the note about to be added. Copied into the event on save,
     * never stored as a reference.
     */
    val quickNoteParticipants: List<EventParticipant> = emptyList(),
    val isLoading: Boolean = false,
    /**
     * What went wrong, as a description the screen formats itself.
     *
     * A [UiText] and not a `String`: the message is produced in the ViewModel, where
     * the locale is none of its business, and a string baked in there would stay in
     * the old language after the user switches the system one. [UiText.Raw] carries
     * the few messages that cannot be translated anyway — the text of an exception.
     */
    val error: UiText? = null,
    val editingEvent: SessionEvent? = null,
    // Edit dialog fields
    val editTitle: String = "",
    val editType: SessionEventType = SessionEventType.NOTE,
    val editContent: String = "",
    /**
     * The cast the edit dialog will write back.
     *
     * Seeded from the event being edited, because the repository replaces the
     * stored snapshot with whatever it is handed: saving an untouched dialog has
     * to hand back the same list, otherwise editing the text of an event would
     * wipe its participants.
     */
    val editParticipants: List<EventParticipant> = emptyList(),
    // Delete confirmation dialog: the event the user is about to delete
    val eventPendingDeletion: SessionEvent? = null,
    // Timeline filtering (in memory, no extra query)
    val searchQuery: String = "",
    /** `null` means "every type", otherwise only events of that type are shown. */
    val typeFilter: SessionEventType? = null
) {
    /**
     * Whether the quick note is worth adding, by the app's one definition of it:
     * [isQuickNoteAddable].
     *
     * The rule itself lives with the component that enforces it — `QuickNoteInput`
     * decides the state of its own Add button from the text it is handed, rather
     * than being told. This property is the same rule read off the state, so that
     * the ViewModel's guard on
     * [onAddQuickNote][com.example.digitaldungeon.ui.session.viewmodel.SessionDetailViewModel.onAddQuickNote]
     * and the button can never disagree about what "empty" means.
     *
     * Participants play no part in it: a note with a cast but no text is still
     * nothing worth storing, and a note without a cast is the normal case.
     */
    val canAddQuickNote: Boolean
        get() = isQuickNoteAddable(quickNoteText)

    /**
     * False for a campaign that has no participants at all — then the quick note
     * shows no participant block instead of an empty one.
     *
     * Names the rule the picker enforces itself by refusing to draw an empty list
     * (`EventParticipantPicker`); it is the same condition, not a second one.
     */
    val canSelectQuickNoteParticipants: Boolean
        get() = campaignParticipants.isNotEmpty()

    /**
     * What the edit dialog offers: the campaign roster plus anyone this event
     * already names who is no longer on it.
     *
     * Those extras matter. An event keeps the cast it was recorded with, so a
     * member who has since been removed from (or renamed in) the campaign still
     * appears in old events. Offering only the current roster would hide them
     * from the dialog while [editParticipants] still carried them — the user
     * could neither see nor deliberately drop them.
     */
    val editParticipantOptions: List<EventParticipant> =
        campaignParticipants + editParticipants.filterNot { it in campaignParticipants }

    /** Nothing to pick from and nothing already picked: hide the block. */
    val canSelectEditParticipants: Boolean
        get() = editParticipantOptions.isNotEmpty()

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
