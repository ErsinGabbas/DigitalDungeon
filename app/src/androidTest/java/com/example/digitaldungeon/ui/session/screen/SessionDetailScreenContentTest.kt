package com.example.digitaldungeon.ui.session.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.session.components.TEST_NOW
import com.example.digitaldungeon.ui.session.components.testEvent
import com.example.digitaldungeon.ui.session.state.SessionDetailUiState
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiPlural
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The seam between the session state and the parts of the screen that render it.
 *
 * This is the layer neither kind of test could reach before the screen was split
 * into [SessionDetailScreen] and [SessionDetailScreenContent]: the ViewModel tests
 * can prove that `canReorder` is false while a filter is on, and the component
 * tests can prove that a card draws the arrow it is told to draw, but nothing
 * could prove that the one actually arrives at the other. `hiltViewModel()` in the
 * middle of the old screen made it unreachable from an instrumented test, so every
 * such connection was held together by reading the code.
 *
 * What is asserted here is therefore the wiring, and only the wiring — the
 * questions whose answers are a two-line edit away from being silently wrong:
 *
 *  - `canReorder` reaches the right arrow of the right card, *with the right
 *    reason*: under a filter every arrow on every card blames the filter, and
 *    without one only the two ends of the list are blocked and they blame their
 *    position.
 *  - the two empty states are not swapped, and never both on screen.
 *  - at most one dialog exists, even for a state that carries two.
 *  - a card's buttons report the event *that* card shows.
 *
 * Every "unavailable" is asserted twice: the control reports itself disabled, and
 * a tap on it reaches nobody.
 */
@RunWith(AndroidJUnit4::class)
class SessionDetailScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ------------------------------------------------------------------- labels

    private val moveUpLabel: String get() = uiString(R.string.event_action_move_up)
    private val moveDownLabel: String get() = uiString(R.string.event_action_move_down)
    private val moveUpFirstLabel: String get() = uiString(R.string.event_action_move_up_first)
    private val moveDownLastLabel: String get() = uiString(R.string.event_action_move_down_last)
    private val moveUpFilteredLabel: String
        get() = uiString(R.string.event_action_move_up_filtered)
    private val moveDownFilteredLabel: String
        get() = uiString(R.string.event_action_move_down_filtered)

    private val emptyTimelineTitle: String
        get() = uiString(R.string.session_timeline_empty_title)
    private val noMatchesTitle: String
        get() = uiString(R.string.session_timeline_no_matches_title)
    private val clearFiltersLabel: String get() = uiString(R.string.session_action_clear_filters)

    private val quickNoteHeading: String get() = uiString(R.string.session_quick_note_heading)
    private val addLabel: String get() = uiString(R.string.session_action_add)
    private val editLabel: String get() = uiString(R.string.session_action_edit)
    private val deleteLabel: String get() = uiString(R.string.session_action_delete)
    private val saveLabel: String get() = uiString(R.string.session_action_save)
    private val cancelLabel: String get() = uiString(R.string.session_action_cancel)

    private val editDialogTitle: String get() = uiString(R.string.event_edit_title)
    private val deleteDialogTitle: String get() = uiString(R.string.event_delete_title)

    private val shareLabel: String get() = uiString(R.string.session_action_share_full_log)
    private val backLabel: String get() = uiString(R.string.session_action_back)
    private val titleFallback: String get() = uiString(R.string.session_detail_title_fallback)

    // --------------------------------------------------------- recorded callbacks

    private val movedUp = mutableListOf<SessionEvent>()
    private val movedDown = mutableListOf<SessionEvent>()
    private val editRequested = mutableListOf<SessionEvent>()
    private val deleteRequested = mutableListOf<SessionEvent>()
    private val savedEventIds = mutableListOf<Int>()
    private var clearFiltersCalls = 0
    private var addNoteCalls = 0
    private var editDismissCalls = 0
    private var deleteConfirmCalls = 0
    private var deleteDismissCalls = 0
    private var shareCalls = 0
    private var backCalls = 0

    /**
     * Renders the screen with every callback recorded, so each test can state only
     * the piece of state it is about and then ask what the screen did with it.
     */
    private fun showScreen(state: SessionDetailUiState) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                SessionDetailScreenContent(
                    state = state,
                    now = TEST_NOW,
                    onBack = { backCalls++ },
                    onShareSessionLog = { shareCalls++ },
                    quickNoteActions = QuickNoteActions(
                        onAdd = { addNoteCalls++ }
                    ),
                    filterActions = TimelineFilterActions(
                        onClearFilters = { clearFiltersCalls++ }
                    ),
                    eventActions = SessionEventActions(
                        onEdit = { editRequested += it },
                        onDelete = { deleteRequested += it },
                        onMoveUp = { movedUp += it },
                        onMoveDown = { movedDown += it }
                    ),
                    editDialogActions = EventEditDialogActions(
                        onSave = { savedEventIds += it },
                        onDismiss = { editDismissCalls++ }
                    ),
                    deleteDialogActions = EventDeleteDialogActions(
                        onConfirm = { deleteConfirmCalls++ },
                        onDismiss = { deleteDismissCalls++ }
                    )
                )
            }
        }
    }

    /**
     * Brings a timeline entry into view. The timeline is the only lazy list on this
     * screen, which is what `hasScrollToKeyAction()` picks out — the type filter row
     * scrolls as well, but sideways and not lazily.
     */
    private fun scrollTimelineTo(text: String) {
        composeTestRule
            .onNode(hasScrollToKeyAction())
            .performScrollToNode(hasText(text))
    }

    /**
     * Every dialog currently on screen, as nodes.
     *
     * "Is a dialog open" is asked of the dialog roots rather than of the wording
     * inside them. The campaign screens have a resource that is a dialog title and
     * a list button's label at the same time, and a text lookup there finds one
     * node per row with no dialog in sight; the session screen's titles happen not
     * to clash today, which is exactly the kind of thing a later rewording changes
     * silently. Counting dialogs is also the stronger claim: not "the other title
     * is absent" but "there is exactly one dialog".
     */
    private val dialogs get() = composeTestRule.onAllNodes(isDialog())

    /**
     * The node with this text *inside the open dialog* - a title, or a button whose
     * label it is. Scoped because the labels are not unique on this screen: every
     * card behind the dialog carries an Edit and a Delete with the very same words.
     */
    private fun inDialog(text: String) =
        composeTestRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    // ============================================================================
    // canReorder reaching the cards — the reason this screen was split in two
    // ============================================================================

    @Test
    fun aFilteredTimelineBlamesTheFilterOnTheFirstCardInsteadOfItsPosition() {
        showScreen(loadedState.copy(searchQuery = MATCHES_EVERY_EVENT))

        composeTestRule
            .onAllNodesWithContentDescription(moveUpFilteredLabel)
            .onFirst()
            .assertIsNotEnabled()

        // The first card is at the top of the list either way, so "already the
        // first event" is the plausible wrong answer here: it is true, it is the
        // reason without the filter, and it names the one thing the user cannot fix
        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).assertDoesNotExist()
    }

    @Test
    fun aFilteredTimelineLeavesNoUsableArrowAnywhereOnTheTimeline() {
        showScreen(loadedState.copy(searchQuery = MATCHES_EVERY_EVENT))

        composeTestRule.onAllNodesWithContentDescription(moveUpLabel).assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription(moveDownLabel).assertCountEquals(0)
    }

    @Test
    fun tappingAnArrowOnAFilteredTimelineDoesNotAskForAMove() {
        showScreen(loadedState.copy(searchQuery = MATCHES_EVERY_EVENT))

        composeTestRule.onAllNodesWithContentDescription(moveUpFilteredLabel)
            .onFirst()
            .performClick()
        composeTestRule.onAllNodesWithContentDescription(moveDownFilteredLabel)
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            "a filtered timeline must not be reordered",
            emptyList<SessionEvent>(),
            movedUp
        )
        assertEquals(
            "a filtered timeline must not be reordered",
            emptyList<SessionEvent>(),
            movedDown
        )
    }

    /**
     * The last card is where "every arrow" has to be taken literally: it is the one
     * whose down arrow is blocked by its position as well, so a screen that forgot
     * `canReorder` for it would still *look* right.
     */
    @Test
    fun aFilteredTimelineBlamesTheFilterOnTheLastCardToo() {
        showScreen(loadedState.copy(searchQuery = MATCHES_EVERY_EVENT))

        scrollTimelineTo(lastEvent.content)

        composeTestRule
            .onAllNodesWithContentDescription(moveDownFilteredLabel)
            .onFirst()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(moveDownLastLabel).assertDoesNotExist()
    }

    @Test
    fun withoutAFilterTheFirstCardIsBlockedOnlyByItsPosition() {
        showScreen(loadedState)

        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(moveUpFilteredLabel).assertDoesNotExist()
    }

    @Test
    fun withoutAFilterTheLastCardIsBlockedOnlyByItsPosition() {
        showScreen(loadedState)

        scrollTimelineTo(lastEvent.content)

        composeTestRule.onNodeWithContentDescription(moveDownLastLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(moveDownFilteredLabel).assertDoesNotExist()
    }

    @Test
    fun tappingTheBlockedArrowOfAnEdgeCardDoesNotAskForAMove() {
        showScreen(loadedState)

        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(emptyList<SessionEvent>(), movedUp)
    }

    /**
     * Two events, so exactly one arrow of each direction is usable and there is no
     * doubt which card it belongs to: the only "move down" on screen is the first
     * event's, and the event it reports is how the screen proves it kept track.
     */
    @Test
    fun theOnlyAvailableDownArrowBelongsToTheFirstCardAndCarriesItsEvent() {
        showScreen(loadedState.copy(events = listOf(firstEvent, secondEvent)))

        composeTestRule.onAllNodesWithContentDescription(moveDownLabel).assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription(moveDownLabel).assertIsEnabled()

        composeTestRule.onNodeWithContentDescription(moveDownLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(firstEvent), movedDown)
        assertEquals(emptyList<SessionEvent>(), movedUp)
    }

    @Test
    fun theOnlyAvailableUpArrowBelongsToTheLastCardAndCarriesItsEvent() {
        showScreen(loadedState.copy(events = listOf(firstEvent, secondEvent)))

        scrollTimelineTo(secondEvent.content)

        composeTestRule.onAllNodesWithContentDescription(moveUpLabel).assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription(moveUpLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(secondEvent), movedUp)
        assertEquals(emptyList<SessionEvent>(), movedDown)
    }

    // ============================================================================
    // The two empty states
    // ============================================================================

    @Test
    fun aTimelineThatNeverHadAnEventSaysSoAndNotThatAFilterHidThem() {
        showScreen(loadedState.copy(events = emptyList()))

        composeTestRule.onNodeWithText(emptyTimelineTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(noMatchesTitle).assertDoesNotExist()
    }

    @Test
    fun aTimelineFilteredToNothingSaysTheEventsAreStillThere() {
        showScreen(loadedState.copy(searchQuery = MATCHES_NO_EVENT))

        composeTestRule.onNodeWithText(noMatchesTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyTimelineTitle).assertDoesNotExist()
    }

    /**
     * Both conditions hold at once for an untouched session the user then searches
     * in. "Your events are still here" would be a lie, so the empty timeline has to
     * win — which is a property of the branch order and of nothing else.
     */
    @Test
    fun anEmptyTimelineKeepsItsOwnMessageWhileAFilterIsActive() {
        showScreen(loadedState.copy(events = emptyList(), searchQuery = MATCHES_NO_EVENT))

        composeTestRule.onNodeWithText(emptyTimelineTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(noMatchesTitle).assertDoesNotExist()
    }

    @Test
    fun aTimelineWithVisibleEventsShowsNeitherEmptyState() {
        showScreen(loadedState)

        composeTestRule.onNodeWithText(emptyTimelineTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(noMatchesTitle).assertDoesNotExist()
    }

    @Test
    fun theWayOutOfAFilteredOutTimelineClearsTheFilters() {
        showScreen(loadedState.copy(searchQuery = MATCHES_NO_EVENT))

        composeTestRule.onNodeWithText(clearFiltersLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, clearFiltersCalls)
    }

    // ============================================================================
    // The filter status line
    // ============================================================================

    @Test
    fun theStatusLineCountsTheVisibleEventsAgainstTheWholeTimeline() {
        showScreen(loadedState.copy(searchQuery = MATCHES_ONLY_THE_FIRST_EVENT))

        composeTestRule
            .onNodeWithText(
                uiPlural(
                    R.plurals.session_filter_status,
                    allEvents.size,
                    1,
                    allEvents.size
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun theStatusLineStaysAwayWhileNothingIsFiltered() {
        showScreen(loadedState)

        composeTestRule
            .onNodeWithText(
                uiPlural(
                    R.plurals.session_filter_status,
                    allEvents.size,
                    allEvents.size,
                    allEvents.size
                )
            )
            .assertDoesNotExist()
    }

    // ============================================================================
    // Dialogs, at most one
    // ============================================================================

    @Test
    fun theEditDialogIsTheOnlyOneOnScreenWhenBothDialogStatesAreSet() {
        showScreen(
            loadedState.copy(
                editingEvent = secondEvent,
                editTitle = secondEvent.title.orEmpty(),
                editContent = secondEvent.content,
                eventPendingDeletion = firstEvent
            )
        )

        dialogs.assertCountEquals(1)
        inDialog(editDialogTitle).assertIsDisplayed()
        inDialog(deleteDialogTitle).assertDoesNotExist()
    }

    @Test
    fun theDeleteConfirmationIsShownWhileNothingIsBeingEdited() {
        showScreen(loadedState.copy(eventPendingDeletion = firstEvent))

        dialogs.assertCountEquals(1)
        inDialog(deleteDialogTitle).assertIsDisplayed()
        inDialog(editDialogTitle).assertDoesNotExist()
    }

    @Test
    fun noDialogIsOnScreenWhileNeitherDialogStateIsSet() {
        showScreen(loadedState)

        dialogs.assertCountEquals(0)
    }

    /**
     * The dialog saves by id, and the id it has to use is the one it was opened
     * with — the event under the dialog, not the first of the list and not whatever
     * card happens to be on top.
     */
    @Test
    fun savingTheEditDialogReportsTheEventItWasOpenedOn() {
        showScreen(
            loadedState.copy(
                editingEvent = secondEvent,
                editTitle = secondEvent.title.orEmpty(),
                editContent = secondEvent.content
            )
        )

        inDialog(saveLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(secondEvent.id), savedEventIds)
    }

    @Test
    fun dismissingTheEditDialogIsReported() {
        showScreen(
            loadedState.copy(
                editingEvent = secondEvent,
                editTitle = secondEvent.title.orEmpty(),
                editContent = secondEvent.content
            )
        )

        inDialog(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, editDismissCalls)
        assertEquals("dismissing the dialog must not save anything", 0, savedEventIds.size)
    }

    @Test
    fun confirmingTheDeleteDialogAsksForTheDeletion() {
        showScreen(loadedState.copy(eventPendingDeletion = firstEvent))

        // Deliberately scoped to the dialog: every card behind it has a Delete
        // button with exactly this label
        inDialog(deleteLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deleteConfirmCalls)
        assertEquals(0, deleteDismissCalls)
    }

    @Test
    fun dismissingTheDeleteDialogCancelsTheDeletion() {
        showScreen(loadedState.copy(eventPendingDeletion = firstEvent))

        inDialog(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deleteDismissCalls)
        assertEquals("a dismissed confirmation must not delete anything", 0, deleteConfirmCalls)
    }

    // ============================================================================
    // The app bar
    // ============================================================================

    @Test
    fun theAppBarShowsTheTitleOfTheLoadedSession() {
        showScreen(loadedState)

        composeTestRule.onNodeWithText(testSession.title).assertIsDisplayed()
    }

    @Test
    fun theAppBarFallsBackToAGenericTitleWhileTheSessionIsUnknown() {
        showScreen(SessionDetailUiState())

        composeTestRule.onNodeWithText(titleFallback).assertIsDisplayed()
    }

    @Test
    fun sharingIsUnavailableBeforeTheSessionHasLoaded() {
        showScreen(SessionDetailUiState())

        composeTestRule.onNodeWithContentDescription(shareLabel).assertIsNotEnabled()
    }

    @Test
    fun tappingShareBeforeTheSessionHasLoadedExportsNothing() {
        showScreen(SessionDetailUiState())

        composeTestRule.onNodeWithContentDescription(shareLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals("there is no log to share yet", 0, shareCalls)
    }

    @Test
    fun sharingBecomesAvailableOnceTheSessionIsLoaded() {
        showScreen(loadedState)

        composeTestRule.onNodeWithContentDescription(shareLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(shareLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, shareCalls)
    }

    @Test
    fun theBackButtonNavigatesUp() {
        showScreen(loadedState)

        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, backCalls)
    }

    // ============================================================================
    // Loading, and the quick note
    // ============================================================================

    @Test
    fun whileTheSessionIsLoadingNeitherTheQuickNoteNorAnEmptyStateIsShown() {
        showScreen(SessionDetailUiState(isLoading = true))

        composeTestRule.onNodeWithText(quickNoteHeading).assertDoesNotExist()
        composeTestRule.onNodeWithText(emptyTimelineTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(noMatchesTitle).assertDoesNotExist()
    }

    @Test
    fun anEmptyNoteInTheStateLeavesTheAddButtonUnavailable() {
        showScreen(loadedState.copy(quickNoteText = "   "))

        composeTestRule.onNodeWithText(addLabel).assertIsNotEnabled()
    }

    @Test
    fun tappingAddWithAnEmptyNoteInTheStateAsksForNothing() {
        showScreen(loadedState.copy(quickNoteText = "   "))

        composeTestRule.onNodeWithText(addLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, addNoteCalls)
    }

    @Test
    fun theNoteTextFromTheStateReachesTheAddButton() {
        showScreen(loadedState.copy(quickNoteText = "The captain surrenders"))

        composeTestRule.onNodeWithText(addLabel).assertIsEnabled()
        composeTestRule.onNodeWithText(addLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, addNoteCalls)
    }

    // ============================================================================
    // A card's buttons report that card's event
    //
    // Every card carries an identically labelled Edit and Delete, so the only thing
    // that can tell them apart is the card they sit on. Each is reached by scrolling
    // its card into view and taking the first or the last match — the last match is
    // the last *composed* card either way, which keeps the lookup right whether or
    // not both cards fit on the device at once.
    // ============================================================================

    @Test
    fun editingTheLastCardReportsThatCardsEvent() {
        showScreen(loadedState.copy(events = listOf(firstEvent, secondEvent)))

        scrollTimelineTo(secondEvent.content)
        composeTestRule.onAllNodesWithText(editLabel).onLast().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(secondEvent), editRequested)
        assertEquals(emptyList<SessionEvent>(), deleteRequested)
    }

    @Test
    fun deletingTheFirstCardReportsThatCardsEvent() {
        showScreen(loadedState.copy(events = listOf(firstEvent, secondEvent)))

        scrollTimelineTo(firstEvent.content)
        composeTestRule.onAllNodesWithText(deleteLabel).onFirst().performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(firstEvent), deleteRequested)
        assertEquals(emptyList<SessionEvent>(), editRequested)
    }

    private companion object {

        /**
         * A word every event mentions, so a search on it leaves the whole timeline
         * visible — the only arrangement in which "every arrow is blocked by the
         * filter" can be told apart from "the list is too short to reorder".
         */
        const val MATCHES_EVERY_EVENT = "Varros"

        /** Matches the first event's content and nothing else. */
        const val MATCHES_ONLY_THE_FIRST_EVENT = "arch"

        const val MATCHES_NO_EVENT = "dragonlance"

        val firstEvent = testEvent(
            id = 1,
            type = SessionEventType.SCENE,
            title = "The Sunken Gate",
            content = "Water drips from the Varros arch."
        )

        val secondEvent = testEvent(
            id = 2,
            type = SessionEventType.DIALOGUE,
            title = "The ferryman",
            content = "The Varros ferryman names a price in memories."
        )

        val lastEvent = testEvent(
            id = 3,
            type = SessionEventType.COMBAT,
            title = "Ambush",
            content = "Three goblins block the Varros crossing."
        )

        val allEvents = listOf(firstEvent, secondEvent, lastEvent)

        val testSession = Session(
            id = 7,
            campaignId = 1,
            title = "Fourth night",
            notes = "",
            createdAt = TEST_NOW
        )

        /**
         * A loaded session with its timeline and nothing else going on. No campaign
         * participants on purpose: the picker draws nothing for an empty roster,
         * which keeps the column short enough for the timeline to have room.
         */
        val loadedState = SessionDetailUiState(
            session = testSession,
            events = allEvents
        )
    }
}
