package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeQuestRepository
import com.example.digitaldungeon.testing.FakeQuestSessionLinkRepository
import com.example.digitaldungeon.testing.FakeSessionEventRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.sessionEvent
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
 * Filtering the timeline.
 *
 * Filtering happens in memory over the already loaded events, so every test also
 * checks that the full `events` list survived: the export and the reordering both
 * read it, and a filter that shrank it would silently corrupt them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelFilterTest {

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

    private fun TestScope.createViewModel(): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            questRepository = questRepository,
            questSessionLinkRepository = questSessionLinkRepository,
            savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
        ).also { advanceUntilIdle() }

    private val stored = listOf(
        sessionEvent(
            id = 1,
            type = SessionEventType.SCENE,
            title = "The Tavern",
            content = "the party meets a stranger",
            orderIndex = 0
        ),
        sessionEvent(
            id = 2,
            type = SessionEventType.COMBAT,
            title = "Goblin Ambush",
            content = "three goblins jump the wagon",
            orderIndex = 1
        ),
        sessionEvent(
            id = 3,
            type = SessionEventType.COMBAT,
            title = null,
            content = "the stranger draws a blade",
            orderIndex = 2
        ),
        sessionEvent(
            id = 4,
            type = SessionEventType.NOTE,
            title = "Loot",
            content = "a rusty key",
            orderIndex = 3
        )
    )

    /** Loads [stored] into the ViewModel through the repository flow. */
    private fun TestScope.viewModelWithEvents(): SessionDetailViewModel {
        eventRepository.events.value = stored
        return createViewModel()
    }

    private fun SessionDetailViewModel.visibleIds(): List<Int> =
        uiState.value.visibleEvents.map { it.id }

    // ---------------------------------------------------------- text filtering

    @Test
    fun withoutAnyFilter_showsEveryEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            assertEquals(stored, viewModel.uiState.value.visibleEvents)
            assertFalse(viewModel.uiState.value.isFilterActive)
        }

    @Test
    fun searchQuery_matchesEventTitle() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("Ambush")

            assertEquals(listOf(2), viewModel.visibleIds())
        }

    @Test
    fun searchQuery_matchesEventContent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("rusty")

            assertEquals(listOf(4), viewModel.visibleIds())
        }

    @Test
    fun searchQuery_matchesContentOfAnUntitledEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            // Event 3 is a quick note: it has no title to match against.
            viewModel.onSearchQueryChange("blade")

            assertEquals(listOf(3), viewModel.visibleIds())
        }

    @Test
    fun searchQuery_ignoresCase() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("gOBLIN")

            assertEquals(listOf(2), viewModel.visibleIds())
        }

    @Test
    fun searchQuery_isTrimmedBeforeMatching() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("  Ambush  ")

            assertEquals(listOf(2), viewModel.visibleIds())
        }

    @Test
    fun searchQuery_canMatchSeveralEvents() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            // "stranger" is in the content of events 1 and 3.
            viewModel.onSearchQueryChange("stranger")

            assertEquals(listOf(1, 3), viewModel.visibleIds())
        }

    @Test
    fun whitespaceOnlySearchQuery_showsEveryEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("   ")

            assertEquals(stored, viewModel.uiState.value.visibleEvents)
            assertFalse(viewModel.uiState.value.isFilterActive)
        }

    // ---------------------------------------------------------- type filtering

    @Test
    fun typeFilter_keepsOnlyEventsOfThatType() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            assertEquals(listOf(2, 3), viewModel.visibleIds())
            assertTrue(viewModel.uiState.value.isFilterActive)
        }

    @Test
    fun typeFilterWithoutMatches_hidesEverything() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onTypeFilterSelected(SessionEventType.DISCOVERY)

            assertTrue(viewModel.uiState.value.visibleEvents.isEmpty())
        }

    @Test
    fun typeFilterSetToNull_showsEveryTypeAgain() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            viewModel.onTypeFilterSelected(null)

            assertEquals(stored, viewModel.uiState.value.visibleEvents)
            assertFalse(viewModel.uiState.value.isFilterActive)
        }

    // ------------------------------------------------- text and type combined

    @Test
    fun searchQueryAndTypeFilter_bothHaveToMatch() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            // "stranger" alone matches events 1 and 3; only 3 is a COMBAT event.
            viewModel.onSearchQueryChange("stranger")
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            assertEquals(listOf(3), viewModel.visibleIds())
        }

    @Test
    fun searchQueryAndTypeFilter_matchingDisjointEvents_hideEverything() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            // "rusty" is only in the NOTE event, so no COMBAT event survives.
            viewModel.onSearchQueryChange("rusty")
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            assertTrue(viewModel.uiState.value.visibleEvents.isEmpty())
        }

    // ------------------------------------------- the full list stays untouched

    @Test
    fun filtering_leavesTheFullEventListIntact() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("rusty")
            viewModel.onTypeFilterSelected(SessionEventType.NOTE)

            assertEquals(stored, viewModel.uiState.value.events)
        }

    @Test
    fun eventsEmission_whileFiltering_isStillFiltered() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()
            viewModel.onSearchQueryChange("goblin")

            eventRepository.events.value = stored + sessionEvent(
                id = 5,
                type = SessionEventType.COMBAT,
                title = "More Goblins",
                content = "reinforcements",
                orderIndex = 4
            )
            advanceUntilIdle()

            assertEquals(listOf(2, 5), viewModel.visibleIds())
            assertEquals(5, viewModel.uiState.value.events.size)
        }

    // ------------------------------------------------------- clearing filters

    @Test
    fun onClearFilters_restoresTheFullTimeline() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()
            viewModel.onSearchQueryChange("goblin")
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            viewModel.onClearFilters()

            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertNull(state.typeFilter)
            assertEquals(stored, state.visibleEvents)
            assertFalse(state.isFilterActive)
        }

    @Test
    fun onClearSearchQuery_keepsTheTypeFilter() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()
            viewModel.onSearchQueryChange("goblin")
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            viewModel.onClearSearchQuery()

            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertEquals(SessionEventType.COMBAT, state.typeFilter)
            assertEquals(listOf(2, 3), viewModel.visibleIds())
        }

    // ----------------------------------------------- the two empty states differ

    @Test
    fun withoutAnyEvent_reportsTimelineEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertTrue(state.isTimelineEmpty)
            assertFalse(state.hasNoMatchingEvents)
        }

    @Test
    fun filterMatchingNothing_reportsNoMatchingEventsNotAnEmptyTimeline() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("a dragon that never showed up")

            val state = viewModel.uiState.value
            assertTrue(state.visibleEvents.isEmpty())
            assertTrue(state.hasNoMatchingEvents)
            // The events are still recorded, the filter just hid them.
            assertFalse(state.isTimelineEmpty)
        }

    @Test
    fun filterMatchingNothing_onAnEmptyTimeline_reportsOnlyTimelineEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("anything")

            val state = viewModel.uiState.value
            assertTrue(state.isTimelineEmpty)
            assertFalse(state.hasNoMatchingEvents)
        }

    @Test
    fun filterWithMatches_reportsNeitherEmptyState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()

            viewModel.onSearchQueryChange("goblin")

            val state = viewModel.uiState.value
            assertFalse(state.isTimelineEmpty)
            assertFalse(state.hasNoMatchingEvents)
        }

    @Test
    fun onClearFilters_afterAnEmptyResult_clearsNoMatchingEvents() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = viewModelWithEvents()
            viewModel.onSearchQueryChange("a dragon that never showed up")
            assertTrue(viewModel.uiState.value.hasNoMatchingEvents)

            viewModel.onClearFilters()

            assertFalse(viewModel.uiState.value.hasNoMatchingEvents)
        }

    private companion object {
        const val SESSION_ID = 7
        const val SESSION_ID_KEY = "sessionId"
    }
}
