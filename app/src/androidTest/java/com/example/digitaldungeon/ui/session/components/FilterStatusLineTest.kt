package com.example.digitaldungeon.ui.session.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiPlural
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one line that says how much of the timeline is being hidden.
 *
 * Two counts go in and one sentence comes out, which is exactly the shape a
 * swapped pair of arguments survives unnoticed: "showing 21 of 3 events" is a
 * perfectly well formed sentence. Both numbers are therefore asserted through the
 * same resource the component uses, with two different values, so an exchange
 * fails here rather than in a screenshot nobody compares.
 *
 * The grammatical number is its own assertion. It follows the *total*, not the
 * visible count — "showing 1 of 5 events" is the correct English and the correct
 * Russian, while a plural chosen by the visible count would read "of 5 event".
 */
@RunWith(AndroidJUnit4::class)
class FilterStatusLineTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun showStatusLine(visibleCount: Int, totalCount: Int) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                FilterStatusLine(
                    visibleCount = visibleCount,
                    totalCount = totalCount
                )
            }
        }
    }

    @Test
    fun namesHowManyEventsOfTheWholeTimelineSurvivedTheFilter() {
        showStatusLine(visibleCount = 3, totalCount = 21)

        composeTestRule
            .onNodeWithText(uiPlural(R.plurals.session_filter_status, 21, 3, 21))
            .assertIsDisplayed()
    }

    @Test
    fun theGrammaticalNumberFollowsTheTotalAndNotTheVisibleCount() {
        showStatusLine(visibleCount = 1, totalCount = 5)

        composeTestRule
            .onNodeWithText(uiPlural(R.plurals.session_filter_status, 5, 1, 5))
            .assertIsDisplayed()

        // The same two numbers in the form a count of one would have picked
        composeTestRule
            .onNodeWithText(uiPlural(R.plurals.session_filter_status, 1, 1, 5))
            .assertDoesNotExist()
    }
}
