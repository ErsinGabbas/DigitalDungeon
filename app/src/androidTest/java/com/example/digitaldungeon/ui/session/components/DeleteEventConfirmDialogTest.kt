package com.example.digitaldungeon.ui.session.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * The dialog that decides whether the right row is about to be deleted.
 *
 * Its summary logic is private to the composable, so a JVM test cannot reach it:
 * the title if there is one, otherwise the beginning of the content, otherwise a
 * placeholder. All three branches matter in practice, because quick notes are
 * created without a title and are exactly the events most likely to be deleted
 * right after being written. A dialog that showed nothing identifying would turn
 * "delete the wrong event" into a silent, unrecoverable mistake.
 */
@RunWith(AndroidJUnit4::class)
class DeleteEventConfirmDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val confirmLabel: String get() = uiString(R.string.session_action_delete)
    private val cancelLabel: String get() = uiString(R.string.session_action_cancel)

    private var confirmCalls = 0
    private var dismissCalls = 0

    private fun showDialog(event: SessionEvent) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                DeleteEventConfirmDialog(
                    event = event,
                    onConfirm = { confirmCalls++ },
                    onDismiss = { dismissCalls++ }
                )
            }
        }
    }

    @Test
    fun namesTheEventByItsTitleAndSaysTheDeleteIsFinal() {
        showDialog(
            testEvent(
                type = SessionEventType.COMBAT,
                title = "Ambush at the bridge",
                content = "Three goblins block the crossing."
            )
        )

        composeTestRule.onNodeWithText(uiString(R.string.event_delete_title)).assertExists()
        composeTestRule.onNodeWithText(uiString(R.string.event_delete_message)).assertExists()
        composeTestRule.onNodeWithText(uiString(R.string.event_type_combat)).assertExists()
        composeTestRule.onNodeWithText("Ambush at the bridge").assertExists()
    }

    @Test
    fun fallsBackToTheContentForAnUntitledQuickNote() {
        showDialog(
            testEvent(
                type = SessionEventType.NOTE,
                title = null,
                content = "The innkeeper still owes the party 40 gold."
            )
        )

        composeTestRule
            .onNodeWithText("The innkeeper still owes the party 40 gold.")
            .assertExists()
    }

    @Test
    fun treatsABlankTitleAsNoTitleAtAll() {
        showDialog(
            testEvent(
                title = "   ",
                content = "A bell rings once below the arch."
            )
        )

        composeTestRule.onNodeWithText("A bell rings once below the arch.").assertExists()
    }

    @Test
    fun showsAPlaceholderForAnEventWithNothingInItAtAll() {
        showDialog(testEvent(title = null, content = "   "))

        composeTestRule
            .onNodeWithText(uiString(R.string.event_delete_empty_summary))
            .assertExists()
    }

    @Test
    fun shortensALongContentInsteadOfQuotingTheWholeNote() {
        // The summary is capped so the dialog cannot grow past its buttons; the
        // ellipsis is what tells the reader the note continues
        val longContent = "x".repeat(150)
        showDialog(testEvent(title = null, content = longContent))

        composeTestRule.onNodeWithText(longContent).assertDoesNotExist()
        composeTestRule.onNodeWithText("x".repeat(SUMMARY_MAX_LENGTH) + "…").assertExists()
    }

    @Test
    fun confirmingReportsTheDeletionAndNothingElse() {
        showDialog(testEvent())

        composeTestRule.onNodeWithText(confirmLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCalls)
        assertEquals("cancelling must not be reported too", 0, dismissCalls)
    }

    @Test
    fun cancellingReportsTheDismissalAndNeverTheDeletion() {
        showDialog(testEvent())

        composeTestRule.onNodeWithText(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissCalls)
        assertEquals("the deletion must not be reported", 0, confirmCalls)
    }

    private companion object {
        /** Mirrors the cap the dialog applies to a content-based summary. */
        const val SUMMARY_MAX_LENGTH = 120
    }
}
