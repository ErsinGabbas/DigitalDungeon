package com.example.digitaldungeon.ui.session.components

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
 * Which chip is marked here is the only thing that tells the user a filter is on
 * at all — and a filter being on is also what switches reordering off. A row that
 * marked nothing would leave the greyed-out arrows on the cards unexplained.
 */
@RunWith(AndroidJUnit4::class)
class EventTypeFilterRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val allLabel: String get() = uiString(R.string.event_type_filter_all)

    private fun typeLabel(type: SessionEventType): String = uiString(eventTypeLabelRes(type))

    private val reported = mutableListOf<SessionEventType?>()

    private fun showRow(selectedType: SessionEventType?) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                EventTypeFilterRow(
                    selectedType = selectedType,
                    onTypeSelected = { reported += it }
                )
            }
        }
    }

    @Test
    fun allIsMarkedWhileNoTypeFilterIsSet() {
        showRow(selectedType = null)

        composeTestRule.onNodeWithText(allLabel).assertIsSelected()
        SessionEventType.entries.forEach { type ->
            composeTestRule.onNodeWithText(typeLabel(type)).assertIsNotSelected()
        }
    }

    @Test
    fun theFilteredTypeIsMarkedAndAllIsNot() {
        showRow(selectedType = SessionEventType.COMBAT)

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.COMBAT)).assertIsSelected()
        composeTestRule.onNodeWithText(allLabel).assertIsNotSelected()
        SessionEventType.entries
            .filter { it != SessionEventType.COMBAT }
            .forEach { type ->
                composeTestRule.onNodeWithText(typeLabel(type)).assertIsNotSelected()
            }
    }

    @Test
    fun tappingATypeAsksForThatFilter() {
        showRow(selectedType = null)

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.SCENE)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<SessionEventType?>(SessionEventType.SCENE), reported)
    }

    @Test
    fun tappingTheActiveTypeClearsTheFilter() {
        showRow(selectedType = SessionEventType.SCENE)

        composeTestRule.onNodeWithText(typeLabel(SessionEventType.SCENE)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<SessionEventType?>(null), reported)
    }

    @Test
    fun tappingAllClearsTheFilter() {
        showRow(selectedType = SessionEventType.DISCOVERY)

        composeTestRule.onNodeWithText(allLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<SessionEventType?>(null), reported)
    }
}
