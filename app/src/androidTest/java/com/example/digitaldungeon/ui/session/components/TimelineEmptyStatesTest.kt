package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
 * Two empty screens that mean opposite things.
 *
 * "No events yet" says the log is empty and the way out is to write something.
 * "No events match the filter" says the log is full and the way out is to stop
 * filtering — and the events are still there, which is the reassurance that keeps
 * someone from re-typing a session they think they lost.
 *
 * Nothing but this test stands between the two: they are picked by neighbouring
 * branches of one `when`, take the same shape on screen, and swapping them is a
 * two-line edit that no ViewModel test can see. Each case therefore also asserts
 * the *absence* of the other one's wording, which fails as well if the two
 * resources are ever given the same text.
 */
@RunWith(AndroidJUnit4::class)
class TimelineEmptyStatesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val emptyTitle: String get() = uiString(R.string.session_timeline_empty_title)
    private val emptyMessage: String get() = uiString(R.string.session_timeline_empty_message)
    private val noMatchesTitle: String
        get() = uiString(R.string.session_timeline_no_matches_title)
    private val noMatchesMessage: String
        get() = uiString(R.string.session_timeline_no_matches_message)
    private val clearFiltersLabel: String get() = uiString(R.string.session_action_clear_filters)

    @Test
    fun theNeverUsedTimelineSaysNothingHasBeenRecordedYet() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EmptyTimelineState(modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(emptyMessage).assertIsDisplayed()

        composeTestRule.onNodeWithText(noMatchesTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(noMatchesMessage).assertDoesNotExist()
    }

    @Test
    fun theNeverUsedTimelineOffersNoFilterToClear() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EmptyTimelineState(modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.onNodeWithText(clearFiltersLabel).assertDoesNotExist()
    }

    @Test
    fun theFilteredOutTimelineSaysTheEventsAreStillThere() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                NoMatchingEventsState(
                    onClearFilters = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeTestRule.onNodeWithText(noMatchesTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(noMatchesMessage).assertIsDisplayed()

        composeTestRule.onNodeWithText(emptyTitle).assertDoesNotExist()
        composeTestRule.onNodeWithText(emptyMessage).assertDoesNotExist()
    }

    @Test
    fun theFilteredOutTimelineOffersAWayBackToTheWholeLog() {
        var clearCalls = 0
        composeTestRule.setContent {
            DigitalDungeonTheme {
                NoMatchingEventsState(
                    onClearFilters = { clearCalls++ },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeTestRule.onNodeWithText(clearFiltersLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(clearFiltersLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, clearCalls)
    }
}
