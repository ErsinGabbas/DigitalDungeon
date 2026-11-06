package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The wall in front of a restore, which is the single most destructive thing the
 * app can do: it replaces the whole database, so every campaign recorded after the
 * chosen backup was taken is gone with no undo.
 *
 * Two things are therefore asserted beyond the callbacks. That the warning text is
 * on screen — it is the only place the app admits that "restore" does not merge.
 * And that the confirm button carries the caller's own label rather than falling
 * back to the generic one, because the whole point of the custom label is that the
 * last thing read before the tap says what the tap does.
 */
@RunWith(AndroidJUnit4::class)
class ConfirmRestoreDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val title: String get() = uiString(R.string.backup_restore_warning_title)
    private val warning: String get() = uiString(R.string.backup_restore_warning_text)
    private val confirmLabel: String get() = uiString(R.string.backup_restore_warning_confirm)
    private val cancelLabel: String get() = uiString(R.string.common_cancel)

    private var confirmCalls = 0
    private var dismissCalls = 0

    private fun showDialog() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                ConfirmRestoreDialog(
                    onConfirm = { confirmCalls++ },
                    onDismiss = { dismissCalls++ }
                )
            }
        }
    }

    @Test
    fun warnsThatRestoringReplacesEverythingInsteadOfMerging() {
        showDialog()

        composeTestRule.onNodeWithText(title).assertExists()
        composeTestRule.onNodeWithText(warning).assertExists()
    }

    @Test
    fun theConfirmButtonSaysWhatItDoesRatherThanJustDelete() {
        showDialog()

        composeTestRule.onNodeWithText(confirmLabel).assertExists()
        composeTestRule.onNodeWithText(uiString(R.string.common_delete)).assertDoesNotExist()
        composeTestRule.onNodeWithText(cancelLabel).assertExists()
    }

    @Test
    fun acceptingTheWarningReportsTheConfirmationAndNothingElse() {
        showDialog()

        composeTestRule.onNodeWithText(confirmLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCalls)
        assertEquals("backing out must not be reported too", 0, dismissCalls)
    }

    @Test
    fun backingOutReportsTheDismissalAndNeverTheRestore() {
        showDialog()

        composeTestRule.onNodeWithText(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissCalls)
        assertEquals("the restore must not be reported", 0, confirmCalls)
    }
}
