package com.example.digitaldungeon.ui.components

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
 * The last stop in front of a cascading delete.
 *
 * The two buttons are a few pixels apart and lead to opposite outcomes, one of
 * which takes every session and event of a campaign with it. Wiring them to the
 * same callback, or to each other's, is a one-character mistake that compiles and
 * that no ViewModel test can notice — the ViewModel is only ever told "confirmed".
 * So each tap is asserted twice: the callback that must fire, and the one that
 * must stay silent.
 *
 * The warning text is asserted as present as well, because that sentence is where
 * the cascade is spelled out; a dialog that only asks "Delete campaign?" does not
 * say that the sessions go too.
 */
@RunWith(AndroidJUnit4::class)
class ConfirmDeleteDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val title: String get() = uiString(R.string.campaign_delete_dialog_title)
    private val warning: String
        get() = uiString(R.string.campaign_delete_dialog_text, CAMPAIGN_NAME)
    private val defaultConfirmLabel: String get() = uiString(R.string.common_delete)
    private val defaultDismissLabel: String get() = uiString(R.string.common_cancel)

    private var confirmCalls = 0
    private var dismissCalls = 0

    private fun showDialog(
        confirmLabel: String? = null,
        dismissLabel: String? = null
    ) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                if (confirmLabel == null || dismissLabel == null) {
                    ConfirmDeleteDialog(
                        title = title,
                        text = warning,
                        onConfirm = { confirmCalls++ },
                        onDismiss = { dismissCalls++ }
                    )
                } else {
                    ConfirmDeleteDialog(
                        title = title,
                        text = warning,
                        onConfirm = { confirmCalls++ },
                        onDismiss = { dismissCalls++ },
                        confirmLabel = confirmLabel,
                        dismissLabel = dismissLabel
                    )
                }
            }
        }
    }

    @Test
    fun showsTheQuestionAndTheWarningAboutWhatElseGoes() {
        showDialog()

        composeTestRule.onNodeWithText(title).assertExists()
        composeTestRule.onNodeWithText(warning).assertExists()
    }

    @Test
    fun offersDeleteAndCancelUnlessTheCallerSaysOtherwise() {
        showDialog()

        composeTestRule.onNodeWithText(defaultConfirmLabel).assertExists()
        composeTestRule.onNodeWithText(defaultDismissLabel).assertExists()
    }

    @Test
    fun confirmingReportsTheConfirmationAndNothingElse() {
        showDialog()

        composeTestRule.onNodeWithText(defaultConfirmLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCalls)
        assertEquals("cancelling must not be reported too", 0, dismissCalls)
    }

    @Test
    fun cancellingReportsTheDismissalAndNeverTheDelete() {
        showDialog()

        composeTestRule.onNodeWithText(defaultDismissLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissCalls)
        assertEquals("the delete must not be reported", 0, confirmCalls)
    }

    @Test
    fun usesTheButtonLabelsTheCallerPassedInstead() {
        val customConfirm = uiString(R.string.backup_restore_warning_confirm)
        showDialog(confirmLabel = customConfirm, dismissLabel = defaultDismissLabel)

        composeTestRule.onNodeWithText(customConfirm).assertExists()
        composeTestRule.onNodeWithText(defaultConfirmLabel).assertDoesNotExist()

        composeTestRule.onNodeWithText(customConfirm).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCalls)
    }

    private companion object {
        const val CAMPAIGN_NAME = "Curse of Strahd"
    }
}
