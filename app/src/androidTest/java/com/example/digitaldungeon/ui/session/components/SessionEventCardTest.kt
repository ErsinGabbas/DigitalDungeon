package com.example.digitaldungeon.ui.session.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The visual half of the reordering guarantee, and the explanation that comes
 * with it.
 *
 * `SessionDetailViewModel` refuses to reorder a filtered timeline, and unit tests
 * already hold it to that. What they cannot see is the card: a card that drew
 * *enabled* arrows and relied on the ViewModel to swallow the taps would look
 * exactly like a working one, and the user would be dragging events around a
 * stored order they are not being shown. So both halves are asserted here — the
 * arrow reports itself unavailable, and tapping it reports nothing.
 *
 * The second half is new and is the reason [EventMoveAvailability] replaced a pair
 * of booleans. A `Boolean` could only say *that* an arrow was dead; the two
 * reasons need two different sentences, and a test against the old flags passed
 * just as happily when both of them were wired to the same wording. So every
 * blocked arrow is checked for the reason it carries — in its
 * `contentDescription`, which is all a screen reader has, and in the tooltip the
 * long press opens, which is all anyone else has once the status line above the
 * list has scrolled away. Each case also asserts the *absence* of the other
 * reason's wording, which fails if the three labels are ever collapsed into one.
 *
 * The arrows are also checked asymmetrically (up blocked, down available and the
 * reverse), which is the one arrangement that fails if the two are wired to the
 * wrong button.
 */
@RunWith(AndroidJUnit4::class)
class SessionEventCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The label of an arrow that works ...
    private val moveUpLabel: String get() = uiString(R.string.event_action_move_up)
    private val moveDownLabel: String get() = uiString(R.string.event_action_move_down)

    // ... and the four that say why one does not
    private val moveUpFirstLabel: String get() = uiString(R.string.event_action_move_up_first)
    private val moveDownLastLabel: String get() = uiString(R.string.event_action_move_down_last)
    private val moveUpFilteredLabel: String
        get() = uiString(R.string.event_action_move_up_filtered)
    private val moveDownFilteredLabel: String
        get() = uiString(R.string.event_action_move_down_filtered)

    // The sentences a long press on a blocked arrow opens
    private val blockedFirstReason: String get() = uiString(R.string.event_reorder_blocked_first)
    private val blockedLastReason: String get() = uiString(R.string.event_reorder_blocked_last)
    private val blockedByFilterReason: String
        get() = uiString(R.string.event_reorder_blocked_by_filter)

    private var moveUpCalls = 0
    private var moveDownCalls = 0

    private fun showCard(
        event: SessionEvent = testEvent(),
        moveUp: EventMoveAvailability = EventMoveAvailability.AT_EDGE,
        moveDown: EventMoveAvailability = EventMoveAvailability.AT_EDGE,
        onEdit: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                SessionEventCard(
                    event = event,
                    now = TEST_NOW,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    moveUp = moveUp,
                    moveDown = moveDown,
                    onMoveUp = { moveUpCalls++ },
                    onMoveDown = { moveDownCalls++ }
                )
            }
        }
    }

    /** A long press, the gesture a blocked arrow answers with its reason. */
    private fun longPress(contentDescription: String) {
        composeTestRule
            .onNodeWithContentDescription(contentDescription)
            .performTouchInput { longClick() }
    }

    /** Waits for the tooltip to be on screen rather than assuming it is instant. */
    private fun awaitText(text: String) {
        composeTestRule.waitUntil(TOOLTIP_TIMEOUT_MS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // -------------------------------------------------------- an arrow that works

    @Test
    fun anEventWithNeighboursOnBothSidesOffersBothArrows() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveUpLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(moveDownLabel).assertIsEnabled()
    }

    @Test
    fun tappingAnAvailableUpArrowAsksForTheMoveUpOnly() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveUpLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, moveUpCalls)
        assertEquals("moving up must never ask for a move down", 0, moveDownCalls)
    }

    @Test
    fun tappingAnAvailableDownArrowAsksForTheMoveDownOnly() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveDownLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, moveDownCalls)
        assertEquals("moving down must never ask for a move up", 0, moveUpCalls)
    }

    // -------------------------------------------------- blocked by its own position

    @Test
    fun theFirstEventSaysItsUpArrowIsBlockedByItsPosition() {
        showCard(
            moveUp = EventMoveAvailability.AT_EDGE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).assertIsNotEnabled()

        // Not merely disabled: the label must not still claim it moves anything,
        // and must not blame a filter that is not on
        composeTestRule.onNodeWithContentDescription(moveUpLabel).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(moveUpFilteredLabel).assertDoesNotExist()
    }

    @Test
    fun tappingTheUpArrowOfTheFirstEventDoesNotAskForAMove() {
        showCard(
            moveUp = EventMoveAvailability.AT_EDGE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals("the first event has nowhere to move up to", 0, moveUpCalls)
    }

    @Test
    fun theFirstEventCanStillBeMovedDown() {
        showCard(
            moveUp = EventMoveAvailability.AT_EDGE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        composeTestRule.onNodeWithContentDescription(moveDownLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(moveDownLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, moveDownCalls)
    }

    @Test
    fun theLastEventSaysItsDownArrowIsBlockedByItsPosition() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AT_EDGE
        )

        composeTestRule.onNodeWithContentDescription(moveDownLastLabel).assertIsNotEnabled()

        composeTestRule.onNodeWithContentDescription(moveDownLabel).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(moveDownFilteredLabel).assertDoesNotExist()
    }

    @Test
    fun tappingTheDownArrowOfTheLastEventDoesNotAskForAMove() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AT_EDGE
        )

        composeTestRule.onNodeWithContentDescription(moveDownLastLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals("the last event has nowhere to move down to", 0, moveDownCalls)
    }

    @Test
    fun theLastEventCanStillBeMovedUp() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AT_EDGE
        )

        composeTestRule.onNodeWithContentDescription(moveUpLabel).assertIsEnabled()
        composeTestRule.onNodeWithContentDescription(moveUpLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, moveUpCalls)
    }

    // ------------------------------------------------------- blocked by the filter

    @Test
    fun aFilteredTimelineBlocksBothArrowsAndBlamesTheFilterRatherThanThePosition() {
        showCard(
            moveUp = EventMoveAvailability.FILTER_ACTIVE,
            moveDown = EventMoveAvailability.FILTER_ACTIVE
        )

        composeTestRule.onNodeWithContentDescription(moveUpFilteredLabel).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(moveDownFilteredLabel).assertIsNotEnabled()

        // The reason is the whole point of the type: a filtered arrow must not be
        // reported as "already the first event", which is a different problem with
        // a different way out
        composeTestRule.onNodeWithContentDescription(moveUpFirstLabel).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(moveDownLastLabel).assertDoesNotExist()
    }

    @Test
    fun tappingAnArrowBlockedByTheFilterDoesNotAskForAMove() {
        showCard(
            moveUp = EventMoveAvailability.FILTER_ACTIVE,
            moveDown = EventMoveAvailability.FILTER_ACTIVE
        )

        composeTestRule.onNodeWithContentDescription(moveUpFilteredLabel).performClick()
        composeTestRule.onNodeWithContentDescription(moveDownFilteredLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals("a filtered timeline must not be reordered", 0, moveUpCalls)
        assertEquals("a filtered timeline must not be reordered", 0, moveDownCalls)
    }

    // ---------------------------------------------------- the explanation on the card

    @Test
    fun aBlockedUpArrowExplainsItsPositionOnLongPress() {
        showCard(
            moveUp = EventMoveAvailability.AT_EDGE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        longPress(moveUpFirstLabel)
        awaitText(blockedFirstReason)

        composeTestRule.onNodeWithText(blockedFirstReason).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockedByFilterReason).assertDoesNotExist()
    }

    @Test
    fun aBlockedDownArrowExplainsItsPositionOnLongPress() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AT_EDGE
        )

        longPress(moveDownLastLabel)
        awaitText(blockedLastReason)

        composeTestRule.onNodeWithText(blockedLastReason).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockedByFilterReason).assertDoesNotExist()
    }

    /**
     * The case the tooltip exists for: nothing on the card hints at a filter, and
     * the status line that used to be the only explanation is above a list the user
     * has scrolled past by the time they wonder.
     */
    @Test
    fun anArrowBlockedByTheFilterExplainsTheFilterOnLongPress() {
        showCard(
            moveUp = EventMoveAvailability.FILTER_ACTIVE,
            moveDown = EventMoveAvailability.FILTER_ACTIVE
        )

        longPress(moveUpFilteredLabel)
        awaitText(blockedByFilterReason)

        composeTestRule.onNodeWithText(blockedByFilterReason).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockedFirstReason).assertDoesNotExist()
    }

    /**
     * A working arrow has nothing to excuse, so it must not swallow a long press for
     * a message that would say nothing.
     */
    @Test
    fun anAvailableArrowHasNothingToExcuseOnLongPress() {
        showCard(
            moveUp = EventMoveAvailability.AVAILABLE,
            moveDown = EventMoveAvailability.AVAILABLE
        )

        longPress(moveUpLabel)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(blockedFirstReason).assertDoesNotExist()
        composeTestRule.onNodeWithText(blockedLastReason).assertDoesNotExist()
        composeTestRule.onNodeWithText(blockedByFilterReason).assertDoesNotExist()
    }

    // ------------------------------------------------------------------- content

    @Test
    fun showsTheTypeTitleAndContentOfTheEvent() {
        showCard(
            event = testEvent(
                type = SessionEventType.COMBAT,
                title = "Ambush at the bridge",
                content = "Three goblins block the crossing."
            )
        )

        composeTestRule.onNodeWithText(uiString(R.string.event_type_combat)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ambush at the bridge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Three goblins block the crossing.").assertIsDisplayed()
    }

    @Test
    fun aQuickNoteWithoutATitleStillShowsItsContent() {
        showCard(
            event = testEvent(
                type = SessionEventType.NOTE,
                title = null,
                content = "The innkeeper still owes the party 40 gold."
            )
        )

        composeTestRule.onNodeWithText(uiString(R.string.event_type_note)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("The innkeeper still owes the party 40 gold.")
            .assertIsDisplayed()
    }

    @Test
    fun listsEveryParticipantRecordedOnTheEvent() {
        showCard(
            event = testEvent(participants = listOf(participantThalia, participantBren))
        )

        composeTestRule
            .onNodeWithText(uiString(R.string.event_participants_heading))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                uiString(
                    R.string.event_participant_line,
                    participantThalia.name,
                    participantThalia.level,
                    participantThalia.maxHp
                )
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                uiString(
                    R.string.event_participant_line,
                    participantBren.name,
                    participantBren.level,
                    participantBren.maxHp
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun omitsTheParticipantsBlockWhenTheEventNamesNobody() {
        showCard(event = testEvent(participants = emptyList()))

        composeTestRule
            .onNodeWithText(uiString(R.string.event_participants_heading))
            .assertDoesNotExist()
    }

    // ------------------------------------------------------------------- actions

    @Test
    fun editReportsOnlyTheEditRequest() {
        var editCalls = 0
        var deleteCalls = 0
        showCard(onEdit = { editCalls++ }, onDelete = { deleteCalls++ })

        composeTestRule.onNodeWithText(uiString(R.string.session_action_edit)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, editCalls)
        assertEquals("editing an event must never ask for its deletion", 0, deleteCalls)
    }

    @Test
    fun deleteReportsOnlyTheDeleteRequest() {
        var editCalls = 0
        var deleteCalls = 0
        showCard(onEdit = { editCalls++ }, onDelete = { deleteCalls++ })

        composeTestRule.onNodeWithText(uiString(R.string.session_action_delete)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deleteCalls)
        assertEquals(0, editCalls)
    }

    private companion object {
        /** Generous: the tooltip waits out the long press before it opens. */
        const val TOOLTIP_TIMEOUT_MS = 5_000L
    }
}
