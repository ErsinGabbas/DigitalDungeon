package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.domain.export.SessionMarkdownLabels
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionEventRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.session
import com.example.digitaldungeon.testing.sessionEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Exporting the session log.
 *
 * The one rule that cannot be seen on screen: the Markdown is built from the full
 * timeline, not from whatever the current filter leaves visible. Exporting a
 * truncated log would look exactly like a successful export.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelExportTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var eventRepository: FakeSessionEventRepository
    private lateinit var campaignRepository: FakeCampaignRepository

    @Before
    fun setUp() {
        sessionRepository = FakeSessionRepository()
        eventRepository = FakeSessionEventRepository()
        campaignRepository = FakeCampaignRepository()
    }

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
            type = SessionEventType.NOTE,
            title = "Loot",
            content = "a rusty key",
            orderIndex = 2
        )
    )

    private fun TestScope.createViewModel(): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
        ).also { advanceUntilIdle() }

    // ------------------------------------------------------- nothing to export

    @Test
    fun buildSessionMarkdown_beforeTheSessionIsLoaded_returnsNull() =
        runTest(mainDispatcherRule.testDispatcher) {

            // No emission yet: the flow has not produced a session.
            val viewModel = SessionDetailViewModel(
                sessionRepository = sessionRepository,
                sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
                savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
            )

            assertNull(viewModel.buildSessionMarkdown())
        }

    @Test
    fun buildSessionMarkdown_forAMissingSession_returnsNull() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = null
            eventRepository.events.value = stored

            val viewModel = createViewModel()

            // A deleted session has nothing to export, even with events in state.
            assertNull(viewModel.buildSessionMarkdown())
        }

    // ------------------------------------------------------------- happy path

    @Test
    fun buildSessionMarkdown_afterLoading_returnsTheLog() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value =
                session(id = SESSION_ID, title = "The Long Night", notes = "no survivors")
            eventRepository.events.value = stored

            val viewModel = createViewModel()

            val markdown = viewModel.buildSessionMarkdown()
            assertNotNull(markdown)
            assertTrue(markdown!!.contains("# The Long Night"))
            assertTrue(markdown.contains("no survivors"))
        }

    @Test
    fun buildSessionMarkdown_withoutEvents_stillReturnsTheHeader() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value =
                session(id = SESSION_ID, title = "The Long Night", notes = "")

            val viewModel = createViewModel()

            val markdown = viewModel.buildSessionMarkdown()!!
            assertTrue(markdown.contains("# The Long Night"))
            assertTrue(markdown.contains("Timeline"))
        }

    @Test
    fun buildSessionMarkdown_containsEveryEventOfTheTimeline() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()

            val markdown = viewModel.buildSessionMarkdown()!!
            assertTrue(markdown.contains("The Tavern"))
            assertTrue(markdown.contains("Goblin Ambush"))
            assertTrue(markdown.contains("Loot"))
        }

    // ------------------------------------------- the filter must not shrink it

    @Test
    fun buildSessionMarkdown_withAnActiveSearchQuery_usesTheFullTimeline() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("goblin")
            // Only one event is on screen...
            assertEquals(1, viewModel.uiState.value.visibleEvents.size)

            val markdown = viewModel.buildSessionMarkdown()!!

            // ...but the export still carries all three.
            assertTrue(markdown.contains("The Tavern"))
            assertTrue(markdown.contains("Goblin Ambush"))
            assertTrue(markdown.contains("Loot"))
        }

    @Test
    fun buildSessionMarkdown_withAnActiveTypeFilter_usesTheFullTimeline() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)
            assertEquals(1, viewModel.uiState.value.visibleEvents.size)

            val markdown = viewModel.buildSessionMarkdown()!!

            assertTrue(markdown.contains("The Tavern"))
            assertTrue(markdown.contains("Goblin Ambush"))
            assertTrue(markdown.contains("Loot"))
        }

    @Test
    fun buildSessionMarkdown_withAFilterHidingEverything_stillExportsEveryEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("a dragon that never showed up")
            assertTrue(viewModel.uiState.value.visibleEvents.isEmpty())

            val markdown = viewModel.buildSessionMarkdown()!!

            assertTrue(markdown.contains("The Tavern"))
            assertTrue(markdown.contains("Goblin Ambush"))
            assertTrue(markdown.contains("Loot"))
        }

    @Test
    fun buildSessionMarkdown_isTheSameWithAndWithoutAFilter() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()
            val unfiltered = viewModel.buildSessionMarkdown()

            viewModel.onSearchQueryChange("goblin")
            viewModel.onTypeFilterSelected(SessionEventType.COMBAT)

            assertEquals(unfiltered, viewModel.buildSessionMarkdown())
        }

    // -------------------------------------------------- reflects the live state

    @Test
    fun buildSessionMarkdown_afterANewEventArrives_includesIt() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value = session(id = SESSION_ID, title = "The Long Night")
            eventRepository.events.value = stored

            val viewModel = createViewModel()
            assertFalse(viewModel.buildSessionMarkdown()!!.contains("A Hidden Door"))

            eventRepository.events.value = stored + sessionEvent(
                id = 4,
                type = SessionEventType.DISCOVERY,
                title = "A Hidden Door",
                content = "behind the bookshelf",
                orderIndex = 3
            )
            advanceUntilIdle()

            assertTrue(viewModel.buildSessionMarkdown()!!.contains("A Hidden Door"))
        }

    // --------------------------------------------------------------- wording

    /**
     * The screen resolves the wording from string resources and passes it in, so the
     * log comes out in the language of the app without a `Context` reaching the
     * ViewModel. The default is the English wording, which is what lets this suite
     * call the method without resources at all.
     */
    @Test
    fun buildSessionMarkdown_withGivenLabels_usesTheirWording() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.session.value =
                session(id = SESSION_ID, title = "The Long Night", notes = "")

            val viewModel = createViewModel()

            val markdown = viewModel.buildSessionMarkdown(
                labels = SessionMarkdownLabels(
                    timelineHeading = "Хронология",
                    emptyTimeline = "_Событий нет._"
                )
            )!!

            assertTrue(markdown.contains("## Хронология"))
            assertTrue(markdown.contains("_Событий нет._"))
            assertFalse(markdown.contains("## Timeline"))
        }

    private companion object {
        const val SESSION_ID = 7
        const val SESSION_ID_KEY = "sessionId"
    }
}
