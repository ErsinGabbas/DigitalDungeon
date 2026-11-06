package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two promises the picker's own documentation makes and nothing else enforces.
 *
 * The first is that an empty roster produces *no* output — not a heading above
 * nothing, which is how a campaign with no participants would otherwise look on
 * the quick note block and inside the edit dialog.
 *
 * The second is that the chips are multiple choice. A control that behaved like a
 * radio group would still let the user pick "a" participant, so the bug would only
 * show up as events that quietly name one person out of a party of four.
 */
@RunWith(AndroidJUnit4::class)
class EventParticipantPickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val label: String get() = uiString(R.string.event_participants_label)

    private fun showPicker(
        options: List<EventParticipant>,
        selected: List<EventParticipant>,
        onToggle: (EventParticipant) -> Unit = {}
    ) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                // A tagged host, so "renders nothing" can be asserted as "the
                // slot the picker was given has no content" rather than only as
                // the absence of strings the test happens to know about
                Box(modifier = Modifier.testTag(HOST_TAG)) {
                    EventParticipantPicker(
                        options = options,
                        selected = selected,
                        onToggle = onToggle
                    )
                }
            }
        }
    }

    @Test
    fun drawsNothingAtAllForACampaignWithoutParticipants() {
        showPicker(options = emptyList(), selected = emptyList())

        composeTestRule.onNodeWithTag(HOST_TAG).onChildren().assertCountEquals(0)
        composeTestRule.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun drawsTheLabelAndAChipForEveryOption() {
        showPicker(
            options = listOf(participantThalia, participantBren, participantNix),
            selected = emptyList()
        )

        composeTestRule.onNodeWithText(label).assertIsDisplayed()
        composeTestRule.onNodeWithText(participantThalia.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(participantBren.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(participantNix.name).assertIsDisplayed()
    }

    @Test
    fun marksOnlyTheChipsThatAreSelected() {
        showPicker(
            options = listOf(participantThalia, participantBren, participantNix),
            selected = listOf(participantBren)
        )

        composeTestRule.onNodeWithText(participantBren.name).assertIsSelected()
        composeTestRule.onNodeWithText(participantThalia.name).assertIsNotSelected()
        composeTestRule.onNodeWithText(participantNix.name).assertIsNotSelected()
    }

    @Test
    fun tappingAChipReportsExactlyThatParticipant() {
        val toggled = mutableListOf<EventParticipant>()
        showPicker(
            options = listOf(participantThalia, participantBren, participantNix),
            selected = emptyList(),
            onToggle = { toggled += it }
        )

        composeTestRule.onNodeWithText(participantBren.name).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(participantBren), toggled)
    }

    @Test
    fun severalParticipantsCanBeSelectedAtTheSameTime() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                val selected = remember { mutableStateListOf<EventParticipant>() }
                EventParticipantPicker(
                    options = listOf(participantThalia, participantBren, participantNix),
                    selected = selected,
                    onToggle = { if (!selected.remove(it)) selected.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText(participantThalia.name).performClick()
        composeTestRule.onNodeWithText(participantNix.name).performClick()
        composeTestRule.waitForIdle()

        // Picking a second name must not unpick the first one
        composeTestRule.onNodeWithText(participantThalia.name).assertIsSelected()
        composeTestRule.onNodeWithText(participantNix.name).assertIsSelected()
        composeTestRule.onNodeWithText(participantBren.name).assertIsNotSelected()
    }

    @Test
    fun tappingASelectedChipAgainUnselectsOnlyThatOne() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                val selected = remember {
                    mutableStateListOf(participantThalia, participantNix)
                }
                EventParticipantPicker(
                    options = listOf(participantThalia, participantBren, participantNix),
                    selected = selected,
                    onToggle = { if (!selected.remove(it)) selected.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText(participantThalia.name).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(participantThalia.name).assertIsNotSelected()
        composeTestRule.onNodeWithText(participantNix.name).assertIsSelected()
    }

    @Test
    fun doesNotDrawASelectedParticipantThatIsNotOffered() {
        // Documented behaviour: selection is filtered through the options, which is
        // why the edit dialog has to offer a departed member as an option to keep
        // them visible on an old event
        showPicker(
            options = listOf(participantThalia),
            selected = listOf(participantBren)
        )

        composeTestRule.onNodeWithText(participantBren.name).assertDoesNotExist()
        composeTestRule.onNodeWithText(participantThalia.name).assertIsNotSelected()
    }

    private companion object {
        const val HOST_TAG = "participant-picker-host"
    }
}
