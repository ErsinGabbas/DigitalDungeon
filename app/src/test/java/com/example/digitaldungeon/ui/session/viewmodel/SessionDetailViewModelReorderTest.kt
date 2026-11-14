package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.data.repository.SessionEventRepository.MoveDirection
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeQuestRepository
import com.example.digitaldungeon.testing.FakeQuestSessionLinkRepository
import com.example.digitaldungeon.testing.FakeSessionEventRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
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

/**
 * Moving an event on the timeline.
 *
 * Reordering is the one action here that writes an invisible value (the stored order),
 * so the guards matter more than the happy path: a move must never reach the
 * repository while a filter hides part of the timeline, at the edges of the list, or
 * for an event that is not on the timeline at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelReorderTest {

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

    private val first = sessionEvent(
        id = 1,
        type = SessionEventType.SCENE,
        title = "The Tavern",
        content = "the party meets a stranger",
        orderIndex = 0
    )

    private val middle = sessionEvent(
        id = 2,
        type = SessionEventType.COMBAT,
        title = "Goblin Ambush",
        content = "three goblins jump the wagon",
        orderIndex = 1
    )

    private val last = sessionEvent(
        id = 3,
        type = SessionEventType.NOTE,
        title = "Loot",
        content = "a rusty key",
        orderIndex = 2
    )

    private val stored = listOf(first, middle, last)

    private fun TestScope.createViewModel(): SessionDetailViewModel {
        eventRepository.events.value = stored
        return SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            questRepository = questRepository,
            questSessionLinkRepository = questSessionLinkRepository,
            savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
        ).also { advanceUntilIdle() }
    }

    // --------------------------------------------------------------- happy path

    @Test
    fun onMoveEventUp_movesTheEventUpByItsId() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.MoveEventCall(
                    eventId = 2,
                    direction = MoveDirection.UP
                ),
                eventRepository.moveEventCalls.single()
            )
        }

    @Test
    fun onMoveEventDown_movesTheEventDownByItsId() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventDown(middle)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.MoveEventCall(
                    eventId = 2,
                    direction = MoveDirection.DOWN
                ),
                eventRepository.moveEventCalls.single()
            )
        }

    @Test
    fun onMoveEventDown_onTheFirstEvent_isAllowed() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventDown(first)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.MoveEventCall(
                    eventId = 1,
                    direction = MoveDirection.DOWN
                ),
                eventRepository.moveEventCalls.single()
            )
        }

    @Test
    fun onMoveEventUp_onTheLastEvent_isAllowed() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(last)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.MoveEventCall(
                    eventId = 3,
                    direction = MoveDirection.UP
                ),
                eventRepository.moveEventCalls.single()
            )
        }

    /** The stored order is the single source of truth; nothing is reordered locally. */
    @Test
    fun onMoveEventUp_doesNotReorderTheStateItself() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertEquals(stored, viewModel.uiState.value.events)
        }

    // ------------------------------------------------------------ list edges

    @Test
    fun onMoveEventUp_onTheFirstEvent_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(first)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun onMoveEventDown_onTheLastEvent_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventDown(last)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun onMoveEventUp_withASingleEvent_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.events.value = listOf(first)
            val viewModel = SessionDetailViewModel(
                sessionRepository = sessionRepository,
                sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            questRepository = questRepository,
            questSessionLinkRepository = questSessionLinkRepository,
                savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
            ).also { advanceUntilIdle() }

            viewModel.onMoveEventUp(first)
            viewModel.onMoveEventDown(first)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun movingAtTheEdge_doesNotSetAnError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(first)
            advanceUntilIdle()

            // Refusing at the edge is normal, not a failure worth a message.
            assertNull(viewModel.uiState.value.error)
        }

    // -------------------------------------------------------- unknown event

    @Test
    fun onMoveEventUp_withAnEventNotOnTheTimeline_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(sessionEvent(id = 404, orderIndex = 1))
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun onMoveEventDown_withAnEventNotOnTheTimeline_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventDown(sessionEvent(id = 404, orderIndex = 1))
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    /** The id decides, not the object: a stale copy of a loaded event still moves. */
    @Test
    fun onMoveEventUp_withAStaleCopyOfALoadedEvent_stillMoves() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(middle.copy(content = "an older revision"))
            advanceUntilIdle()

            assertEquals(2, eventRepository.moveEventCalls.single().eventId)
        }

    // ------------------------------------------------- blocked while filtering

    @Test
    fun onMoveEventUp_withAnActiveSearchQuery_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            // The event stays visible, so the button looks usable - the guard is
            // what stops a swap with a neighbour the user cannot see.
            viewModel.onSearchQueryChange("goblin")

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun onMoveEventDown_withAnActiveSearchQuery_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("goblin")

            viewModel.onMoveEventDown(middle)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun onMoveEventUp_withAnActiveTypeFilter_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertTrue(eventRepository.moveEventCalls.isEmpty())
        }

    @Test
    fun activeFilter_disablesReordering() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.canReorder)

            viewModel.onSearchQueryChange("goblin")

            assertFalse(viewModel.uiState.value.canReorder)
        }

    @Test
    fun whitespaceOnlySearchQuery_doesNotBlockReordering() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            // Nothing is hidden, so nothing has to be guarded against.
            viewModel.onSearchQueryChange("   ")

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertEquals(2, eventRepository.moveEventCalls.single().eventId)
        }

    @Test
    fun onClearFilters_allowsReorderingAgain() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("goblin")
            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()
            assertTrue(eventRepository.moveEventCalls.isEmpty())

            viewModel.onClearFilters()
            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.MoveEventCall(
                    eventId = 2,
                    direction = MoveDirection.UP
                ),
                eventRepository.moveEventCalls.single()
            )
        }

    @Test
    fun onMoveEventUp_blockedByAFilter_doesNotSetAnError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("goblin")

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
        }

    // ------------------------------------------------------ repository failure

    @Test
    fun onMoveEventUp_whenRepositoryThrows_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.moveEventError = IllegalStateException("reorder failed")

            val viewModel = createViewModel()

            viewModel.onMoveEventUp(middle)
            advanceUntilIdle()

            assertEquals(UiText.Raw("reorder failed"), viewModel.uiState.value.error)
        }

    @Test
    fun onMoveEventDown_whenRepositoryThrows_setsError() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.moveEventError = IllegalStateException("reorder failed")

            val viewModel = createViewModel()

            viewModel.onMoveEventDown(middle)
            advanceUntilIdle()

            assertEquals(UiText.Raw("reorder failed"), viewModel.uiState.value.error)
        }

    private companion object {
        const val SESSION_ID = 7
        const val SESSION_ID_KEY = "sessionId"
    }
}
