package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepository
import com.example.digitaldungeon.data.repository.SessionEventRepository
import com.example.digitaldungeon.data.repository.SessionEventRepository.MoveDirection
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.domain.export.SessionMarkdownLabels
import com.example.digitaldungeon.domain.export.formatSessionAsMarkdown
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.state.SessionDetailUiState
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionEventRepository: SessionEventRepository,
    private val campaignRepository: CampaignRepository,
    private val questRepository: QuestRepository,
    private val questSessionLinkRepository: QuestSessionLinkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Int = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    /** Which campaign [campaignRosterJob] is currently following, if any. */
    private var observedCampaignId: Int? = null

    private var campaignRosterJob: Job? = null

    /** Follows the quests of the same campaign, for the titles of the linked quests. */
    private var campaignQuestsJob: Job? = null

    init {
        loadSession()
        loadEvents()
        loadQuestLinks()
    }

    /**
     * Which quests came up in this session.
     *
     * Read-only: linking happens on the quest's own screen, where the campaign's quests are
     * already loaded. This screen only needs to be able to cross over.
     */
    private fun loadQuestLinks() {
        viewModelScope.launch {
            questSessionLinkRepository.getLinksForSession(sessionId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.asUiText(
                                R.string.session_error_quests_load_failed
                            )
                        )
                    }
                }
                .collect { links ->
                    _uiState.update { it.copy(questLinks = links) }
                }
        }
    }

    // -------------------------
    // SESSION
    // -------------------------
    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            sessionRepository.getSessionById(sessionId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.toUiText(),
                            isLoading = false
                        )
                    }
                }
                .collect { session ->
                    // session == null is a valid state (session deleted),
                    // so loading must stop on the very first emission
                    _uiState.update {
                        it.copy(
                            session = session,
                            isLoading = false
                        )
                    }

                    // The roster hangs off the campaign, which is only reachable
                    // through the session — so it cannot be requested in init
                    onCampaignIdResolved(session?.campaignId)
                }
        }
    }

    // -------------------------
    // CAMPAIGN ROSTER
    //
    // Only feeds the two pickers. Events are never re-read through it: each one
    // stores its own copy of the cast, and that copy is what the card shows.
    // -------------------------
    /**
     * Re-points the roster subscription when the session finally names its
     * campaign (or names a different one).
     *
     * Guarded by [observedCampaignId] so that an ordinary session edit, which
     * makes `getSessionById` emit again with the same campaign, does not restart
     * the query.
     */
    private fun onCampaignIdResolved(campaignId: Int?) {
        if (campaignId == observedCampaignId) return

        observedCampaignId = campaignId
        campaignRosterJob?.cancel()
        campaignQuestsJob?.cancel()

        if (campaignId == null) {
            onCampaignRosterLoaded(emptyList())
            _uiState.update { it.copy(campaignQuests = emptyList()) }
            return
        }

        campaignQuestsJob = viewModelScope.launch {
            questRepository.getQuestsForCampaign(campaignId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.asUiText(
                                R.string.session_error_quests_load_failed
                            )
                        )
                    }
                }
                .collect { quests ->
                    _uiState.update { it.copy(campaignQuests = quests) }
                }
        }

        campaignRosterJob = viewModelScope.launch {
            try {
                campaignRepository.getCampaignByIdFlow(campaignId)
                    .collect { campaign ->
                        onCampaignRosterLoaded(campaign?.participants ?: emptyList())
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    private fun onCampaignRosterLoaded(roster: List<EventParticipant>) {
        _uiState.update { state ->
            state.copy(
                campaignParticipants = roster,
                // A pending pick must not outlive the person it names: whoever
                // left the campaign before the note was saved drops out of the
                // selection as well, otherwise the chip would vanish while the
                // choice silently stayed.
                quickNoteParticipants = state.quickNoteParticipants.filter { it in roster }
                // editParticipants is deliberately left alone. It is a snapshot
                // the user is editing, not a view of the roster, and pruning it
                // here would erase exactly what this feature has to preserve.
            )
        }
    }

    // -------------------------
    // EVENTS
    // -------------------------
    private fun loadEvents() {
        viewModelScope.launch {
            try {
                sessionEventRepository.getEventsForSession(sessionId)
                    .collect { events ->
                        _uiState.update { it.copy(events = events) }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    // -------------------------
    // FILTERING
    //
    // Purely in memory: the filtered timeline is derived from the already
    // loaded events in SessionDetailUiState, so no extra query (and no schema
    // change) is involved and the full list is never lost.
    // -------------------------
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onClearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    /** Passing `null` shows every type again. */
    fun onTypeFilterSelected(type: SessionEventType?) {
        _uiState.update { it.copy(typeFilter = type) }
    }

    fun onClearFilters() {
        _uiState.update { it.copy(searchQuery = "", typeFilter = null) }
    }

    // -------------------------
    // REORDERING
    // -------------------------
    fun onMoveEventUp(event: SessionEvent) {
        moveEvent(event, MoveDirection.UP)
    }

    fun onMoveEventDown(event: SessionEvent) {
        moveEvent(event, MoveDirection.DOWN)
    }

    /**
     * Shifts [event] one position on the full timeline.
     *
     * Refuses to act while a filter is active: neighbours in the filtered list
     * are not neighbours in the stored order, so a move would quietly swap a
     * pair the user never saw. The screen disables the buttons for the same
     * reason — this guard only makes the rule impossible to bypass.
     */
    private fun moveEvent(event: SessionEvent, direction: MoveDirection) {
        val state = _uiState.value
        if (!state.canReorder) return

        val index = state.events.indexOfFirst { it.id == event.id }
        if (index == -1) return

        val targetIndex = if (direction == MoveDirection.UP) index - 1 else index + 1
        // The edges are already disabled in the UI; keep the ViewModel honest
        // in case the state changed between composition and the click
        if (targetIndex !in state.events.indices) return

        viewModelScope.launch {
            try {
                sessionEventRepository.moveEvent(
                    eventId = event.id,
                    direction = direction
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    // -------------------------
    // EXPORT
    // -------------------------
    /**
     * The session log as Markdown, or `null` while no session is loaded.
     *
     * Always built from the **full** event list, never from the filtered one:
     * exporting the current selection would hand the user a silently truncated
     * log. Returning a plain string keeps every Android concern (the share
     * intent) on the screen side.
     *
     * @param labels the wording of the document. The screen resolves it from string
     *   resources (`sessionMarkdownLabels()`) and passes it in, so the log comes out
     *   in the language of the app without a `Context` reaching this ViewModel or the
     *   pure formatter. Defaults to the English wording, which is what lets a JVM test
     *   call this without resources.
     */
    fun buildSessionMarkdown(
        labels: SessionMarkdownLabels = SessionMarkdownLabels()
    ): String? {
        val state = _uiState.value
        val session = state.session ?: return null

        return formatSessionAsMarkdown(
            session = session,
            events = state.events,
            labels = labels
        )
    }

    // -------------------------
    // QUICK NOTE
    // -------------------------
    fun onQuickNoteTextChanged(text: String) {
        _uiState.update { it.copy(quickNoteText = text) }
    }

    fun onEventTypeSelected(type: SessionEventType) {
        _uiState.update { it.copy(selectedEventType = type) }
    }

    /**
     * Adds or removes [participant] from the cast of the note being written.
     *
     * A toggle rather than a setter, because that is what a chip does, and it
     * keeps "nobody selected" reachable — the usual state of a quick note.
     */
    fun onQuickNoteParticipantToggled(participant: EventParticipant) {
        _uiState.update {
            it.copy(quickNoteParticipants = it.quickNoteParticipants.toggle(participant))
        }
    }

    fun onAddQuickNote() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                // Same rule the Add button is enabled by, so the two can never disagree
                if (!state.canAddQuickNote) return@launch

                sessionEventRepository.addEvent(
                    sessionId = sessionId,
                    type = state.selectedEventType,
                    title = null,
                    content = state.quickNoteText,
                    // Copied into the event here and never consulted again
                    participants = state.quickNoteParticipants
                )

                // The cast is cleared along with the text: carrying it over to
                // the next note would quietly attribute an unrelated entry to
                // the same people. Naming participants stays a deliberate act.
                _uiState.update {
                    it.copy(
                        quickNoteText = "",
                        quickNoteParticipants = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    // -------------------------
    // DELETE
    // -------------------------
    /**
     * Arms the delete confirmation dialog. Nothing is removed until
     * [onConfirmDeleteEvent] is called.
     */
    fun onDeleteEventClick(event: SessionEvent) {
        // Only one dialog at a time: asking to delete closes the edit dialog
        _uiState.update {
            it.clearEditDialog().copy(eventPendingDeletion = event)
        }
    }

    fun onConfirmDeleteEvent() {
        val event = _uiState.value.eventPendingDeletion ?: return

        _uiState.update { it.copy(eventPendingDeletion = null) }

        viewModelScope.launch {
            try {
                sessionEventRepository.deleteEvent(event.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    fun onDeleteDialogDismiss() {
        _uiState.update { it.copy(eventPendingDeletion = null) }
    }

    // -------------------------
    // EDIT
    // -------------------------
    fun onEditEvent(event: SessionEvent) {
        _uiState.update {
            it.copy(
                // Only one dialog at a time: editing closes a pending delete
                eventPendingDeletion = null,
                editingEvent = event,
                editTitle = event.title ?: "",
                editType = event.type,
                editContent = event.content,
                // Seeded from the event, because the save path writes this list
                // back wholesale. Opening the dialog and only touching the text
                // must therefore leave the stored cast exactly as it was.
                editParticipants = event.participants
            )
        }
    }

    fun onEditTitleChange(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun onEditTypeChange(type: SessionEventType) {
        _uiState.update { it.copy(editType = type) }
    }

    fun onEditContentChange(content: String) {
        _uiState.update { it.copy(editContent = content) }
    }

    /**
     * Adds or removes [participant] from the snapshot the dialog will store.
     *
     * Works on [SessionDetailUiState.editParticipants], which may name people the
     * campaign no longer lists; those are offered as options too, so the only way
     * one leaves an event is by being unchecked here.
     */
    fun onEditParticipantToggled(participant: EventParticipant) {
        _uiState.update {
            it.copy(editParticipants = it.editParticipants.toggle(participant))
        }
    }

    fun onUpdateEvent(eventId: Int) {
        viewModelScope.launch {
            try {
                // One snapshot of the state, so the four fields written are the
                // four fields the dialog showed
                val state = _uiState.value

                sessionEventRepository.updateEvent(
                    eventId = eventId,
                    title = state.editTitle,
                    type = state.editType,
                    content = state.editContent,
                    // updateEvent replaces the stored cast with whatever it gets,
                    // so the list has to be passed every time. It starts out as
                    // the event's own participants (see onEditEvent): leaving it
                    // out here is what used to erase them on a text-only edit.
                    participants = state.editParticipants
                )

                _uiState.update { it.clearEditDialog() }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiText()) }
            }
        }
    }

    fun onEditDialogDismiss() {
        _uiState.update { it.clearEditDialog() }
    }

    // -------------------------
    // ERROR
    // -------------------------
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * A failure as something the screen can show.
     *
     * The text of an exception is not translatable — it comes from Room, from the
     * platform, from anywhere but this app — so it travels as [UiText.Raw]. Only the
     * case of no message at all falls back to a sentence of our own, which is where a
     * resource id is both possible and right.
     */
    private fun Throwable.toUiText(): UiText =
        message
            ?.takeIf { it.isNotBlank() }
            ?.let { UiText.Raw(it) }
            ?: UiText.of(R.string.session_error_unknown)

    private fun SessionDetailUiState.clearEditDialog(): SessionDetailUiState =
        copy(
            editingEvent = null,
            editTitle = "",
            editType = SessionEventType.NOTE,
            editContent = "",
            editParticipants = emptyList()
        )

    /**
     * Membership is by value, matching how the chips are drawn: a participant is
     * in or out, never in twice, and the objects put into the list are the ones
     * copied into the event.
     */
    private fun List<EventParticipant>.toggle(
        participant: EventParticipant
    ): List<EventParticipant> =
        if (contains(participant)) {
            filterNot { it == participant }
        } else {
            this + participant
        }
}
