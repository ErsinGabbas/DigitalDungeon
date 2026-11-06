package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.components.formatCreationDate
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The card is the whole campaign list, and it stacks three buttons with no labels
 * in common with each other: Open, Edit and Delete, one of which cascades through
 * every session and event of the campaign. Which callback each one is wired to is
 * invisible to the ViewModel tests, so all three are tapped here and the other two
 * counters are required to stay at zero.
 *
 * The participant block is checked in both directions as well: it carries the
 * party a campaign is actually about, and it must disappear rather than appear
 * empty-headed when there is no party yet.
 */
@RunWith(AndroidJUnit4::class)
class CampaignCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var openCalls = 0
    private var editCalls = 0
    private var deleteCalls = 0

    private fun showCard(campaign: Campaign) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                CampaignCard(
                    campaign = campaign,
                    onEdit = { editCalls++ },
                    onDelete = { deleteCalls++ },
                    onOpen = { openCalls++ }
                )
            }
        }
    }

    @Test
    fun showsTheNameDescriptionAndWhenTheCampaignWasCreated() {
        showCard(campaignWithParty)

        composeTestRule.onNodeWithText(campaignWithParty.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(campaignWithParty.description).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                uiString(
                    R.string.common_created_at,
                    formatCreationDate(campaignWithParty.createdAt)
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun listsEveryParticipantWithTheirLevelAndHitPoints() {
        showCard(campaignWithParty)

        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_participants_card_title))
            .assertIsDisplayed()

        campaignWithParty.participants.forEach { participant ->
            composeTestRule.onNodeWithText(participant.name).assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    uiString(
                        R.string.campaign_participant_stats,
                        participant.level,
                        participant.maxHp
                    )
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun omitsTheParticipantBlockForACampaignWithoutAParty() {
        showCard(campaignWithParty.copy(participants = emptyList()))

        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_participants_card_title))
            .assertDoesNotExist()
    }

    @Test
    fun openReportsOnlyTheOpenRequest() {
        showCard(campaignWithParty)

        composeTestRule.onNodeWithText(uiString(R.string.campaign_open_button)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, openCalls)
        assertEquals(0, editCalls)
        assertEquals("opening a campaign must never ask for a delete", 0, deleteCalls)
    }

    @Test
    fun editReportsOnlyTheEditRequest() {
        showCard(campaignWithParty)

        composeTestRule.onNodeWithText(uiString(R.string.common_edit)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, editCalls)
        assertEquals(0, openCalls)
        assertEquals("editing a campaign must never ask for a delete", 0, deleteCalls)
    }

    @Test
    fun deleteReportsOnlyTheDeleteRequest() {
        showCard(campaignWithParty)

        composeTestRule.onNodeWithText(uiString(R.string.common_delete)).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, deleteCalls)
        assertEquals(0, openCalls)
        assertEquals(0, editCalls)
    }

    private companion object {
        /** 15 June 2024, 12:00 UTC — fixed, so the created-at line is reproducible. */
        const val CREATED_AT: Long = 1_718_452_800_000L

        val campaignWithParty = Campaign(
            id = 1,
            name = "Curse of Strahd",
            description = "Gothic horror in the valley of Barovia",
            createdAt = CREATED_AT,
            // Short names on purpose: a participant row puts the name and the
            // stats at opposite ends of one row, and the test must assert that
            // both are on screen rather than that they fit a particular device
            participants = listOf(
                EventParticipant(name = "Ireena", level = 4, maxHp = 28),
                EventParticipant(name = "Ismark", level = 3, maxHp = 24)
            )
        )
    }
}
