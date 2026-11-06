package com.example.digitaldungeon.ui.session.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Adding a blank note is a no-op in the ViewModel, which is already covered by
 * unit tests. What those cannot tell is whether the button *looks* available
 * while doing nothing — a tap that silently fails reads as a broken app, and the
 * user retypes the note instead of noticing the field is empty.
 *
 * The emptiness rule now lives in the component: [QuickNoteInput] reads it off the
 * text it is handed through [isQuickNoteAddable], and there is no parameter a
 * caller could use to contradict it. So these are assertions about the *rule* and
 * not about the wiring any more — "a line of spaces is nothing" is a promise this
 * component keeps on its own, and the only way to break it is to edit the rule.
 * That was untestable while the verdict arrived as `canAdd` from outside: a test
 * could only confirm that the button obeyed whatever it was told.
 *
 * Every unavailable case is asserted twice over, because the two halves fail
 * independently: the button reports itself disabled, *and* a tap on it reaches
 * nobody. A button that looks dead but still fires is as wrong as the reverse.
 */
@RunWith(AndroidJUnit4::class)
class QuickNoteInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val addLabel: String get() = uiString(R.string.session_action_add)
    private val placeholder: String get() = uiString(R.string.session_quick_note_placeholder)

    private var addCalls = 0

    private fun showInput(text: String) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                QuickNoteInput(
                    text = text,
                    onTextChange = {},
                    onAdd = { addCalls++ }
                )
            }
        }
    }

    // ------------------------------------------------------- nothing worth adding

    @Test
    fun addIsUnavailableWhileNothingHasBeenTyped() {
        showInput(text = "")

        composeTestRule.onNodeWithText(addLabel).assertIsNotEnabled()
    }

    @Test
    fun tappingAddWithAnEmptyNoteDoesNotAskForANote() {
        showInput(text = "")

        composeTestRule.onNodeWithText(addLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, addCalls)
    }

    /**
     * Whitespace is where the rule used to be contradicted: `canAdd = text.isNotEmpty()`
     * from a caller lit the button up for a line of spaces, the ViewModel dropped the
     * note, and the button had lied. The component decides this one itself now.
     */
    @Test
    fun addIsUnavailableForANoteOfNothingButWhitespace() {
        showInput(text = "  \t  ")

        composeTestRule.onNodeWithText(addLabel).assertIsNotEnabled()
    }

    @Test
    fun tappingAddWithAWhitespaceOnlyNoteDoesNotAskForANote() {
        showInput(text = "  \t  ")

        composeTestRule.onNodeWithText(addLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals("a note of nothing but spaces must not be requested", 0, addCalls)
    }

    // ---------------------------------------------------------- something to store

    @Test
    fun addBecomesAvailableForANoteWithActualText() {
        showInput(text = "The bell rings again")

        composeTestRule.onNodeWithText(addLabel).assertIsEnabled()
    }

    @Test
    fun tappingAddWithANoteWorthKeepingAsksForIt() {
        showInput(text = "The bell rings again")

        composeTestRule.onNodeWithText(addLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, addCalls)
    }

    /**
     * "Blank" is not "untrimmed": text that happens to be padded is still text, and
     * refusing it would make the button unusable for a pasted note.
     */
    @Test
    fun addIsAvailableForTextPaddedWithSpaces() {
        showInput(text = "   A bell below the arch   ")

        composeTestRule.onNodeWithText(addLabel).assertIsEnabled()
    }

    // ------------------------------------------------------------------ the field

    @Test
    fun showsThePlaceholderWhileTheNoteIsEmpty() {
        showInput(text = "")

        composeTestRule.onNodeWithText(placeholder).assertIsDisplayed()
    }

    @Test
    fun showsWhatTheUserTypesAndDropsThePlaceholder() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                var text by remember { mutableStateOf("") }
                QuickNoteInput(
                    text = text,
                    onTextChange = { text = it },
                    onAdd = {}
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("A bell below the arch")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("A bell below the arch").assertIsDisplayed()
        composeTestRule.onNodeWithText(placeholder).assertDoesNotExist()
    }

    /**
     * The state of the button follows the text as it is typed, which is the case the
     * screen actually produces: nothing is retyped, the same component simply gets a
     * longer string every keystroke.
     */
    @Test
    fun addTurnsAvailableAsSoonAsTheTypedNoteStopsBeingBlank() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                var text by remember { mutableStateOf(" ") }
                QuickNoteInput(
                    text = text,
                    onTextChange = { text = it },
                    onAdd = { addCalls++ }
                )
            }
        }

        composeTestRule.onNodeWithText(addLabel).assertIsNotEnabled()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("a")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(addLabel).assertIsEnabled()
    }
}
