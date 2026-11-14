package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeQuestRepository
import com.example.digitaldungeon.testing.FakeQuestSessionLinkRepository
import com.example.digitaldungeon.testing.FakeSessionEventRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.session
import com.example.digitaldungeon.testing.sessionEvent
import com.example.digitaldungeon.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var eventRepository: FakeSessionEventRepository
    private lateinit var campaignRepository: FakeCampaignRepository
    private lateinit var questRepository: FakeQuestRepository
    private lateinit var questSessionLinkRepository: FakeQuestSessionLinkRepository

    @Before
    fun setUp() {
        sessionRepository = FakeSessionRepository()
        eventRepository = FakeSessionEventRepository()
        campaignRepository = FakeCampaignRepository()
        questRepository = FakeQuestRepository()
        questSessionLinkRepository = FakeQuestSessionLinkRepository()
    }

    /** Creates the ViewModel and lets the `init` subscriptions settle. */
    private fun TestScope.createViewModel(sessionId: Int = SESSION_ID): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            questRepository = questRepository,
            questSessionLinkRepository = questSessionLinkRepository,
            savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to sessionId))
        ).also { advanceUntilIdle() }

    // ------------------------------------------------- navigation argument

    @Test
    fun init_readsSessionIdFromSavedStateHandle() =
        runTest(mainDispatcherRule.testDispatcher) {

            createViewModel(sessionId = 7)

            assertEquals(listOf(7), sessionRepository.getSessionByIdCalls)
            assertEquals(listOf(7), eventRepository.getEventsForSessionCalls)
        }

    @Test(expected = IllegalStateException::class)
    fun init_withoutSessionIdArgument_fails() {
        SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            questRepository = questRepository,
            questSessionLinkRepository = questSessionLinkRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    // ---------------------------------------------------------- session load

    @Test
    fun init_loadsSessionAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = session(id = SESSION_ID, title = "The Long Night")
            sessionRepository.session.value = stored

            val viewModel = createViewModel()

            assertEquals(stored, viewModel.uiState.value.session)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    /** A deleted session is a legitimate answer - the screen must not spin forever. */
    @Test
    fun missingSession_clearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = null

            val viewModel = createViewModel()

            assertNull(viewModel.uiState.value.session)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun sessionFlowFailure_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.getSessionByIdError = IllegalStateException("session query failed")

            val viewModel = createViewModel()

            assertEquals(UiText.Raw("session query failed"), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // ----------------------------------------------------------- event load

    @Test
    fun eventsEmission_updatesState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.events.isEmpty())

            val stored = listOf(sessionEvent(id = 1), sessionEvent(id = 2))
            eventRepository.events.value = stored
            advanceUntilIdle()

            assertEquals(stored, viewModel.uiState.value.events)
        }

    @Test
    fun eventsFlowFailure_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.getEventsForSessionError = IllegalStateException("event query failed")

            val viewModel = createViewModel()

            assertEquals(UiText.Raw("event query failed"), viewModel.uiState.value.error)
        }

    // ----------------------------------------------------------- quick note

    @Test
    fun onAddQuickNote_withEmptyText_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertTrue(eventRepository.addEventCalls.isEmpty())
        }

    @Test
    fun onAddQuickNote_withWhitespaceOnlyText_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("    ")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertTrue(eventRepository.addEventCalls.isEmpty())
            assertEquals("    ", viewModel.uiState.value.quickNoteText)
        }

    @Test
    fun onAddQuickNote_withText_callsRepositoryWithSelectedType() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onEventTypeSelected(SessionEventType.COMBAT)
            viewModel.onQuickNoteTextChanged("the goblin rolls a natural 20")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.AddEventCall(
                    sessionId = SESSION_ID,
                    type = SessionEventType.COMBAT,
                    title = null,
                    content = "the goblin rolls a natural 20"
                ),
                eventRepository.addEventCalls.single()
            )
        }

    @Test
    fun onAddQuickNote_onSuccess_clearsQuickNoteText() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("the goblin rolls a natural 20")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.quickNoteText)
        }

    @Test
    fun onAddQuickNote_onSuccess_keepsSelectedType() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onEventTypeSelected(SessionEventType.DISCOVERY)
            viewModel.onQuickNoteTextChanged("a hidden door")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(SessionEventType.DISCOVERY, viewModel.uiState.value.selectedEventType)
        }

    @Test
    fun onAddQuickNote_whenRepositoryThrows_setsErrorAndKeepsText() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.addEventError = IllegalStateException("insert failed")

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("the goblin rolls a natural 20")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(UiText.Raw("insert failed"), viewModel.uiState.value.error)
            assertEquals("the goblin rolls a natural 20", viewModel.uiState.value.quickNoteText)
        }

    // --------------------------------------------------------- delete event

    @Test
    fun onDeleteEventClick_armsPendingDeletionWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val event = sessionEvent(id = 9)
            val viewModel = createViewModel()

            viewModel.onDeleteEventClick(event)
            advanceUntilIdle()

            assertEquals(event, viewModel.uiState.value.eventPendingDeletion)
            assertTrue(eventRepository.deleteEventCalls.isEmpty())
        }

    @Test
    fun onConfirmDeleteEvent_deletesArmedEventAndClearsPendingDeletion() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onDeleteEventClick(sessionEvent(id = 9))

            viewModel.onConfirmDeleteEvent()
            advanceUntilIdle()

            assertEquals(listOf(9), eventRepository.deleteEventCalls)
            assertNull(viewModel.uiState.value.eventPendingDeletion)
        }

    @Test
    fun onConfirmDeleteEvent_withoutArmedEvent_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onConfirmDeleteEvent()
            advanceUntilIdle()

            assertTrue(eventRepository.deleteEventCalls.isEmpty())
        }

    @Test
    fun onDeleteDialogDismiss_clearsPendingDeletionWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onDeleteEventClick(sessionEvent(id = 9))

            viewModel.onDeleteDialogDismiss()
            viewModel.onConfirmDeleteEvent()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.eventPendingDeletion)
            assertTrue(eventRepository.deleteEventCalls.isEmpty())
        }

    @Test
    fun onConfirmDeleteEvent_whenRepositoryThrows_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.deleteEventError = IllegalStateException("delete failed")

            val viewModel = createViewModel()
            viewModel.onDeleteEventClick(sessionEvent(id = 9))

            viewModel.onConfirmDeleteEvent()
            advanceUntilIdle()

            assertEquals(UiText.Raw("delete failed"), viewModel.uiState.value.error)
        }

    // ----------------------------------------------- mutually exclusive dialogs

    @Test
    fun onDeleteEventClick_closesEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val edited = sessionEvent(
                id = 1,
                type = SessionEventType.SCENE,
                title = "A Tavern",
                content = "the usual"
            )
            val viewModel = createViewModel()
            viewModel.onEditEvent(edited)

            viewModel.onDeleteEventClick(sessionEvent(id = 2))

            val state = viewModel.uiState.value
            assertNull(state.editingEvent)
            assertEquals("", state.editTitle)
            assertEquals("", state.editContent)
            assertEquals(SessionEventType.NOTE, state.editType)
            assertEquals(sessionEvent(id = 2), state.eventPendingDeletion)
        }

    @Test
    fun onEditEvent_clearsPendingDeletion() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onDeleteEventClick(sessionEvent(id = 2))

            viewModel.onEditEvent(sessionEvent(id = 1))

            assertNull(viewModel.uiState.value.eventPendingDeletion)
            assertEquals(sessionEvent(id = 1), viewModel.uiState.value.editingEvent)
        }

    // ----------------------------------------------------------- edit event

    @Test
    fun onEditEvent_fillsEditFieldsFromEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val event = sessionEvent(
                id = 4,
                type = SessionEventType.DIALOGUE,
                title = "The Duke Speaks",
                content = "he lies"
            )
            val viewModel = createViewModel()

            viewModel.onEditEvent(event)

            val state = viewModel.uiState.value
            assertEquals(event, state.editingEvent)
            assertEquals("The Duke Speaks", state.editTitle)
            assertEquals(SessionEventType.DIALOGUE, state.editType)
            assertEquals("he lies", state.editContent)
        }

    @Test
    fun onEditEvent_withoutTitle_startsWithEmptyTitleField() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onEditEvent(sessionEvent(id = 4, title = null))

            assertEquals("", viewModel.uiState.value.editTitle)
        }

    @Test
    fun onUpdateEvent_sendsEditedValuesToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val event = sessionEvent(
                id = 4,
                type = SessionEventType.NOTE,
                title = "old title",
                content = "old content"
            )
            val viewModel = createViewModel()

            viewModel.onEditEvent(event)
            viewModel.onEditTitleChange("new title")
            viewModel.onEditTypeChange(SessionEventType.COMBAT)
            viewModel.onEditContentChange("new content")

            viewModel.onUpdateEvent(event.id)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.UpdateEventCall(
                    eventId = 4,
                    title = "new title",
                    type = SessionEventType.COMBAT,
                    content = "new content"
                ),
                eventRepository.updateEventCalls.single()
            )
        }

    @Test
    fun onUpdateEvent_onSuccess_closesEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val event = sessionEvent(id = 4, title = "old title", content = "old content")
            val viewModel = createViewModel()

            viewModel.onEditEvent(event)
            viewModel.onEditTitleChange("new title")

            viewModel.onUpdateEvent(event.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingEvent)
            assertEquals("", state.editTitle)
            assertEquals("", state.editContent)
            assertEquals(SessionEventType.NOTE, state.editType)
        }

    @Test
    fun onUpdateEvent_whenRepositoryThrows_setsErrorAndKeepsDialogOpen() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.updateEventError = IllegalStateException("update failed")

            val event = sessionEvent(id = 4, title = "old title", content = "old content")
            val viewModel = createViewModel()

            viewModel.onEditEvent(event)
            viewModel.onEditTitleChange("new title")

            viewModel.onUpdateEvent(event.id)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(UiText.Raw("update failed"), state.error)
            assertEquals(event, state.editingEvent)
            assertEquals("new title", state.editTitle)
        }

    @Test
    fun onEditDialogDismiss_closesEditDialogWithoutUpdating() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onEditEvent(sessionEvent(id = 4, title = "old title"))
            viewModel.onEditTitleChange("new title")

            viewModel.onEditDialogDismiss()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.editingEvent)
            assertEquals("", viewModel.uiState.value.editTitle)
            assertTrue(eventRepository.updateEventCalls.isEmpty())
        }

    // ---------------------------------------------------------------- error

    @Test
    fun clearError_removesErrorFromState() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.addEventError = IllegalStateException("insert failed")

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("something")
            viewModel.onAddQuickNote()
            advanceUntilIdle()
            assertEquals(UiText.Raw("insert failed"), viewModel.uiState.value.error)

            viewModel.clearError()

            assertNull(viewModel.uiState.value.error)
        }

    /**
     * Room and the platform both throw messageless exceptions, and a banner reading
     * "null" explains nothing - so the one case where a sentence of our own is both
     * possible and right falls back to a resource.
     */
    @Test
    fun repositoryFailureWithoutAMessage_fallsBackToALocalizedError() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.addEventError = IllegalStateException()

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("something")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(
                UiText.of(R.string.session_error_unknown),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun repositoryFailureWithABlankMessage_fallsBackToALocalizedError() =
        runTest(mainDispatcherRule.testDispatcher) {

            // A blank message is as useless as none at all, and must not reach the screen.
            eventRepository.addEventError = IllegalStateException("   ")

            val viewModel = createViewModel()
            viewModel.onQuickNoteTextChanged("something")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(
                UiText.of(R.string.session_error_unknown),
                viewModel.uiState.value.error
            )
        }

    private companion object {
        const val SESSION_ID = 7

        /**
         * The ViewModel hardcodes this key instead of exposing a constant, so the test
         * has to repeat the literal; keep the two in sync.
         */
        const val SESSION_ID_KEY = "sessionId"
    }
}
