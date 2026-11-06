package com.example.digitaldungeon.ui.session.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The type a new event will get is shown by one thing only: which chip is marked
 * selected. Chips that all look the same would leave the quick note block with no
 * visible answer to "what am I about to write", and the type is not editable
 * afterwards without opening the edit dialog.
 *
 * Asserted through the selected *semantics* rather than through colors, which are
 * a palette decision and may be restyled at any time.
 */
@RunWith(AndroidJUnit4::class)
class EventTypeSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun typeLabel(type: SessionEventType): String = uiString(eventTypeLabelRes(type))

    @Test
    fun marksTheCurrentTypeAndLeavesEveryOtherTypeUnmarked() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EventTypeSelector(
                    selectedType = SessionEventType.DIALOGUE,
                    onTypeSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.DIALOGUE)).assertIsSelected()
        SessionEventType.entries
            .filter { it != SessionEventType.DIALOGUE }
            .forEach { type ->
                composeTestRule.onNodeWithText(typeLabel(type)).assertIsNotSelected()
            }
    }

    @Test
    fun tappingATypeReportsThatType() {
        val reported = mutableListOf<SessionEventType>()
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EventTypeSelector(
                    selectedType = SessionEventType.SCENE,
                    onTypeSelected = { reported += it }
                )
            }
        }

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.DISCOVERY)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(SessionEventType.DISCOVERY), reported)
    }

    @Test
    fun theMarkMovesToTheTypeTheUserPicks() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                var selected by remember { mutableStateOf(SessionEventType.SCENE) }
                EventTypeSelector(
                    selectedType = selected,
                    onTypeSelected = { selected = it }
                )
            }
        }

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.SCENE)).assertIsSelected()

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.COMBAT)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.COMBAT)).assertIsSelected()
        composeTestRule.onNodeWithText(typeLabel(SessionEventType.SCENE)).assertIsNotSelected()
    }

    @Test
    fun thereIsNoWayToEndUpWithNoTypeAtAll() {
        // Unlike the filter row, the selector has no "All" state: tapping the
        // active chip re-reports the same type instead of clearing it
        val reported = mutableListOf<SessionEventType>()
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EventTypeSelector(
                    selectedType = SessionEventType.NOTE,
                    onTypeSelected = { reported += it }
                )
            }
        }

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.NOTE)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(SessionEventType.NOTE), reported)
        composeTestRule.onNodeWithText(typeLabel(SessionEventType.NOTE)).assertIsSelected()
        composeTestRule.onNodeWithText(uiString(R.string.event_type_filter_all)).assertDoesNotExist()
    }
}
