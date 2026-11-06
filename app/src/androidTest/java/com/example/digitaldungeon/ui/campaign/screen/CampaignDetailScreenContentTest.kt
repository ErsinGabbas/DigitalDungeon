package com.example.digitaldungeon.ui.campaign.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.ui.campaign.components.CampaignParticipantActions
import com.example.digitaldungeon.ui.campaign.components.CampaignSessionActions
import com.example.digitaldungeon.ui.campaign.state.CampaignDetailUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignParticipantsUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignSessionsUiState
import com.example.digitaldungeon.ui.campaign.state.PendingSessionDeletion
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import com.example.digitaldungeon.ui.uiString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The seam between the campaign detail screen's *three* states and what it draws,
 * reachable now that [CampaignDetailScreenContent] exists without
 * `hiltViewModel()` in front of it.
 *
 * Three flows exist for one reason: the campaign, its party and its sessions fail
 * independently, and one failure must not silence another. That is a claim about
 * the screen and nothing else — each state object knows only its own error, so no
 * ViewModel test can tell whether the screen gives all three a place of their own.
 * Here all three are set at once and each message is required to be on screen.
 *
 * The other claims checked here are of the same kind: the unsaved-party flag is
 * the only warning standing between a rearranged party and a navigate-up that
 * drops it; the two session dialogs must never be stacked; and the participant
 * row callbacks must carry an *index*, because a party may legitimately contain
 * two members with the same name and then nothing but the index says which row
 * was pressed.
 *
 * This screen is one `LazyColumn`, so anything below the party has to be scrolled
 * into view before it can be asserted as displayed — an item that was never
 * composed is not "hidden", it does not exist.
 */
@RunWith(AndroidJUnit4::class)
class CampaignDetailScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ------------------------------------------------------------------- labels

    private val titleFallback: String get() = uiString(R.string.campaign_detail_title_fallback)
    private val backLabel: String get() = uiString(R.string.common_back)

    private val participantsHeading: String
        get() = uiString(R.string.campaign_participants_heading)
    private val unsavedLabel: String get() = uiString(R.string.campaign_participants_unsaved)
    private val noParticipantsMessage: String
        get() = uiString(R.string.campaign_participants_empty)
    private val saveParticipantsLabel: String
        get() = uiString(R.string.campaign_participants_save)
    private val addParticipantLabel: String get() = uiString(R.string.campaign_participant_add)
    private val updateParticipantLabel: String
        get() = uiString(R.string.campaign_participant_update)
    private val editLabel: String get() = uiString(R.string.common_edit)
    private val removeLabel: String get() = uiString(R.string.common_remove)

    private val addSessionLabel: String get() = uiString(R.string.campaign_session_add_button)
    private val sessionEditDialogTitle: String
        get() = uiString(R.string.campaign_session_edit_dialog_title)
    private val sessionDeleteDialogTitle: String
        get() = uiString(R.string.campaign_session_delete_dialog_title)
    private val deleteLabel: String get() = uiString(R.string.common_delete)
    private val cancelLabel: String get() = uiString(R.string.common_cancel)

    private fun participantSummary(participant: EventParticipant): String =
        uiString(
            R.string.campaign_participant_summary,
            participant.name,
            participant.level,
            participant.maxHp
        )

    // --------------------------------------------------------- recorded callbacks

    private val editedIndexes = mutableListOf<Int>()
    private val removedIndexes = mutableListOf<Int>()
    private var saveParticipantsCalls = 0
    private val openedSessions = mutableListOf<Int>()
    private val sessionEditRequested = mutableListOf<Session>()
    private val sessionDeleteRequested = mutableListOf<Session>()
    private var addSessionCalls = 0
    private var confirmDeleteSessionCalls = 0
    private var cancelDeleteSessionCalls = 0
    private var backCalls = 0

    private fun showScreen(
        campaignState: CampaignDetailUiState = loadedCampaign,
        participantsState: CampaignParticipantsUiState = savedParty,
        sessionsState: CampaignSessionsUiState = loadedSessions
    ) {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                CampaignDetailScreenContent(
                    campaignState = campaignState,
                    participantsState = participantsState,
                    sessionsState = sessionsState,
                    onBack = { backCalls++ },
                    onOpenSession = { openedSessions += it },
                    participantActions = CampaignParticipantActions(
                        onEditParticipant = { editedIndexes += it },
                        onRemoveParticipant = { removedIndexes += it },
                        onSave = { saveParticipantsCalls++ }
                    ),
                    sessionActions = CampaignSessionActions(
                        onAddSession = { addSessionCalls++ },
                        onEditSessionRequest = { sessionEditRequested += it },
                        onDeleteSessionRequest = { sessionDeleteRequested += it },
                        onConfirmDeleteSession = { confirmDeleteSessionCalls++ },
                        onCancelDeleteSession = { cancelDeleteSessionCalls++ }
                    )
                )
            }
        }
    }

    /**
     * Brings an item of the screen's single [androidx.compose.foundation.lazy.LazyColumn]
     * into view. Everything below the party needs this: the screen is one lazy list,
     * and an item that has never been composed cannot be displayed.
     */
    private fun scrollTo(text: String) {
        composeTestRule
            .onNode(hasScrollToKeyAction())
            .performScrollToNode(hasText(text))
    }

    /**
     * Every dialog currently on screen, as nodes.
     *
     * "Is a dialog open" is asked of the dialogs themselves and never of the text
     * inside them: `campaign_session_edit_dialog_title` and
     * `campaign_session_edit_action` are the same sentence in every locale the app
     * ships ("Edit session" / "Изменить сессию"), so looking for that text finds
     * one node per session row whether or not a dialog exists. Counting dialog
     * roots cannot be fooled by a label, and it is also the strongest form of the
     * claim being made: not "the other title is absent" but "there is exactly one
     * dialog".
     */
    private val dialogs get() = composeTestRule.onAllNodes(isDialog())

    /**
     * The node with this text *inside the open dialog* - a title, or a button
     * whose label it is. Scoped, for the same reason as [dialogs]: the text alone
     * is ambiguous against the list behind the dialog.
     */
    private fun inDialog(text: String) =
        composeTestRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    // ============================================================================
    // The app bar
    // ============================================================================

    @Test
    fun theAppBarShowsTheNameOfTheLoadedCampaign() {
        showScreen()

        composeTestRule.onNodeWithText(strahd.name).assertIsDisplayed()
    }

    @Test
    fun theAppBarFallsBackToAGenericTitleWhileTheCampaignIsUnknown() {
        showScreen(
            campaignState = CampaignDetailUiState(
                campaignId = 404,
                error = UiText.of(R.string.campaign_not_found)
            ),
            participantsState = CampaignParticipantsUiState(),
            sessionsState = CampaignSessionsUiState()
        )

        composeTestRule.onNodeWithText(titleFallback).assertIsDisplayed()
    }

    @Test
    fun theBackButtonNavigatesUp() {
        showScreen()

        composeTestRule.onNodeWithContentDescription(backLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, backCalls)
    }

    // ============================================================================
    // Three error channels, three places
    // ============================================================================

    /**
     * The state the split into three flows exists for. A single error field would
     * show whichever failure happened last and quietly drop the other two, so all
     * three are set here and all three have to survive each other.
     */
    @Test
    fun allThreeErrorChannelsAreOnScreenAtTheSameTime() {
        val campaignError = uiString(R.string.campaign_error_load_one_failed)
        val participantsError = uiString(R.string.campaign_error_participants_save_failed)
        val sessionsError = uiString(R.string.campaign_error_session_delete_failed)

        showScreen(
            campaignState = loadedCampaign.copy(
                error = UiText.of(R.string.campaign_error_load_one_failed)
            ),
            participantsState = savedParty.copy(
                error = UiText.of(R.string.campaign_error_participants_save_failed)
            ),
            sessionsState = loadedSessions.copy(
                error = UiText.of(R.string.campaign_error_session_delete_failed)
            )
        )

        scrollTo(sessionsError)
        composeTestRule.onNodeWithText(sessionsError).assertIsDisplayed()

        scrollTo(campaignError)
        composeTestRule.onNodeWithText(campaignError).assertIsDisplayed()

        scrollTo(participantsError)
        composeTestRule.onNodeWithText(participantsError).assertIsDisplayed()
    }

    @Test
    fun aCampaignThatLoadedCleanlyShowsNoErrorAtAll() {
        showScreen()

        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_error_load_one_failed))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_error_participants_save_failed))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(uiString(R.string.campaign_error_session_delete_failed))
            .assertDoesNotExist()
    }

    // ============================================================================
    // The party
    // ============================================================================

    /**
     * The party is edited in memory and saved as a whole, so this flag is the only
     * thing standing between a rearranged party and a navigate-up that drops it.
     */
    @Test
    fun aPartyEditedButNotSavedIsFlaggedNextToTheHeading() {
        showScreen(
            participantsState = CampaignParticipantsUiState(
                participants = party.dropLast(1),
                savedParticipants = party
            )
        )

        composeTestRule.onNodeWithText(unsavedLabel).assertIsDisplayed()
    }

    @Test
    fun aPartyThatMatchesWhatIsStoredIsNotFlagged() {
        showScreen(participantsState = savedParty)

        composeTestRule.onNodeWithText(unsavedLabel).assertDoesNotExist()
    }

    @Test
    fun aCampaignWithoutAPartySaysSoInsteadOfShowingAnEmptyBlock() {
        showScreen(participantsState = CampaignParticipantsUiState())

        scrollTo(noParticipantsMessage)
        composeTestRule.onNodeWithText(noParticipantsMessage).assertIsDisplayed()
    }

    @Test
    fun aCampaignWithAPartyListsItsMembersAndClaimsNoEmptyParty() {
        showScreen(participantsState = savedParty)

        composeTestRule.onNodeWithText(participantSummary(ireena)).assertIsDisplayed()
        composeTestRule.onNodeWithText(participantSummary(ismark)).assertIsDisplayed()
        composeTestRule.onNodeWithText(noParticipantsMessage).assertDoesNotExist()
    }

    /**
     * Two party members with the same name, the same level and the same hit points
     * are two different rows of one editable collection, and the row callbacks take
     * a position for exactly this case: nothing else on screen can tell them apart,
     * so an off-by-one or a `indexOf(participant)` would edit the wrong one.
     */
    @Test
    fun editingTheSecondOfTwoIdenticalPartyMembersReportsItsIndex() {
        showScreen(participantsState = partyOfTwins)

        scrollTo(participantsHeading)
        composeTestRule.onAllNodesWithText(editLabel).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(editLabel)[1].performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(1), editedIndexes)
        assertEquals("editing a row must never remove it", emptyList<Int>(), removedIndexes)
    }

    @Test
    fun removingTheFirstOfTwoIdenticalPartyMembersReportsItsIndex() {
        showScreen(participantsState = partyOfTwins)

        scrollTo(participantsHeading)
        composeTestRule.onAllNodesWithText(removeLabel).assertCountEquals(2)
        composeTestRule.onAllNodesWithText(removeLabel)[0].performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(0), removedIndexes)
        assertEquals(
            "removing a row must never open it for editing",
            emptyList<Int>(),
            editedIndexes
        )
    }

    @Test
    fun savingThePartyIsReported() {
        showScreen()

        scrollTo(saveParticipantsLabel)
        composeTestRule.onNodeWithText(saveParticipantsLabel)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, saveParticipantsCalls)
    }

    /**
     * The form has two mutually exclusive confirm buttons, and which one it shows is
     * decided by `editingIndex` alone - offering "Add" while a row is being edited
     * would append a copy instead of updating it.
     */
    @Test
    fun theParticipantFormOffersAnUpdateWhileARowIsBeingEdited() {
        showScreen(
            participantsState = savedParty.copy(
                participantName = ismark.name,
                participantLevel = ismark.level.toString(),
                participantMaxHp = ismark.maxHp.toString(),
                editingIndex = 1
            )
        )

        scrollTo(updateParticipantLabel)
        composeTestRule.onNodeWithText(updateParticipantLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(addParticipantLabel).assertDoesNotExist()
    }

    @Test
    fun theParticipantFormOffersAnAddWhileNoRowIsBeingEdited() {
        showScreen()

        scrollTo(addParticipantLabel)
        composeTestRule.onNodeWithText(addParticipantLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText(updateParticipantLabel).assertDoesNotExist()
    }

    // ============================================================================
    // Sessions
    // ============================================================================

    @Test
    fun theSessionListShowsEverySessionOfTheCampaign() {
        showScreen()

        scrollTo(deathHouse.title)
        composeTestRule.onNodeWithText(deathHouse.title).assertIsDisplayed()

        scrollTo(barovia.title)
        composeTestRule.onNodeWithText(barovia.title).assertIsDisplayed()
    }

    @Test
    fun openingASessionRowReportsThatSessionsId() {
        showScreen()

        scrollTo(barovia.title)
        composeTestRule.onNodeWithText(barovia.title)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(barovia.id), openedSessions)
        assertEquals(
            "opening a session must never ask for its deletion",
            emptyList<Session>(),
            sessionDeleteRequested
        )
    }

    /**
     * Every session row carries an identically labelled "Edit session", so the row
     * it sits on is the only thing that can tell them apart. The last match is the
     * last *composed* row either way, which keeps the lookup right however much of
     * the list the device has room for.
     *
     * Two scrolls, and both are needed. The first one composes the last row at all
     * - it is far below the fold, and an item a lazy list has never composed cannot
     * be found by any matcher. The second brings *the button* into view: scrolling
     * to the session's card stops as soon as the card is visible, and the button
     * sits in a row underneath it, which can still leave it past the bottom edge
     * with nothing to click.
     *
     * No dialog is open here, so the only nodes carrying this text are the rows'.
     */
    @Test
    fun editingASessionRowReportsThatSession() {
        showScreen()

        scrollTo(barovia.title)
        composeTestRule
            .onAllNodesWithText(uiString(R.string.campaign_session_edit_action))
            .onLast()
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(barovia), sessionEditRequested)
        assertEquals(
            "editing a session must never ask for its deletion",
            emptyList<Session>(),
            sessionDeleteRequested
        )
    }

    @Test
    fun addingASessionIsReported() {
        showScreen()

        scrollTo(addSessionLabel)
        composeTestRule.onNodeWithText(addSessionLabel)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, addSessionCalls)
    }

    // ============================================================================
    // Dialogs, at most one
    // ============================================================================

    @Test
    fun theSessionEditDialogIsTheOnlyOneOnScreenWhenBothDialogStatesAreSet() {
        showScreen(
            sessionsState = loadedSessions.copy(
                editingSession = deathHouse,
                editSessionTitle = deathHouse.title,
                editSessionNotes = deathHouse.notes,
                pendingDeletion = PendingSessionDeletion(
                    sessionId = barovia.id,
                    sessionTitle = barovia.title
                )
            )
        )

        dialogs.assertCountEquals(1)
        inDialog(sessionEditDialogTitle).assertIsDisplayed()
        inDialog(sessionDeleteDialogTitle).assertDoesNotExist()
    }

    @Test
    fun theSessionDeleteConfirmationIsShownWhileNothingIsBeingEdited() {
        showScreen(
            sessionsState = loadedSessions.copy(
                pendingDeletion = PendingSessionDeletion(
                    sessionId = deathHouse.id,
                    sessionTitle = deathHouse.title
                )
            )
        )

        dialogs.assertCountEquals(1)
        inDialog(sessionDeleteDialogTitle).assertIsDisplayed()
        inDialog(sessionEditDialogTitle).assertDoesNotExist()
    }

    /**
     * Asked of the dialog roots and not of any wording: every session row carries
     * an "Edit session" button, which is the edit dialog's title word for word, so
     * an absent dialog cannot be established by the absence of that text.
     */
    @Test
    fun noDialogIsOnScreenWhileNeitherDialogStateIsSet() {
        showScreen()

        dialogs.assertCountEquals(0)
    }

    @Test
    fun theSessionDeleteConfirmationNamesTheSessionItIsAbout() {
        showScreen(
            sessionsState = loadedSessions.copy(
                pendingDeletion = PendingSessionDeletion(
                    sessionId = barovia.id,
                    sessionTitle = barovia.title
                )
            )
        )

        inDialog(uiString(R.string.campaign_session_delete_dialog_text, barovia.title))
            .assertIsDisplayed()
    }

    @Test
    fun confirmingTheSessionDeletionAsksForTheDeletion() {
        showScreen(
            sessionsState = loadedSessions.copy(
                pendingDeletion = PendingSessionDeletion(
                    sessionId = barovia.id,
                    sessionTitle = barovia.title
                )
            )
        )

        // Scoped to the dialog on purpose: "Delete" is also the label on every
        // session card behind it
        inDialog(deleteLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmDeleteSessionCalls)
        assertEquals(0, cancelDeleteSessionCalls)
    }

    @Test
    fun dismissingTheSessionDeleteConfirmationCancelsTheDeletion() {
        showScreen(
            sessionsState = loadedSessions.copy(
                pendingDeletion = PendingSessionDeletion(
                    sessionId = barovia.id,
                    sessionTitle = barovia.title
                )
            )
        )

        inDialog(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, cancelDeleteSessionCalls)
        assertEquals(
            "a dismissed confirmation must not delete anything",
            0,
            confirmDeleteSessionCalls
        )
    }

    private companion object {

        /** 15 June 2024, 12:00 UTC — fixed, so the created-at lines are reproducible. */
        const val CREATED_AT: Long = 1_718_452_800_000L

        val ireena = EventParticipant(name = "Ireena", level = 4, maxHp = 28)
        val ismark = EventParticipant(name = "Ismark", level = 3, maxHp = 24)

        val party = listOf(ireena, ismark)

        /**
         * Two rows that are identical in every visible way. The index in the
         * callback is the only thing that can tell them apart, which is precisely
         * what the row callbacks promise.
         */
        val twins = listOf(
            EventParticipant(name = "Mirt", level = 2, maxHp = 18),
            EventParticipant(name = "Mirt", level = 2, maxHp = 18)
        )

        val strahd = Campaign(
            id = 1,
            name = "Curse of Strahd",
            description = "Gothic horror in the valley of Barovia",
            createdAt = CREATED_AT,
            participants = party
        )

        val deathHouse = Session(
            id = 11,
            campaignId = 1,
            title = "Session 3 - Death House",
            notes = "The nursery, and what the party found under it.",
            createdAt = CREATED_AT
        )

        val barovia = Session(
            id = 12,
            campaignId = 1,
            title = "Session 4 - Barovia by day",
            notes = "An escort to Vallaki is asked for.",
            createdAt = CREATED_AT
        )

        val loadedCampaign = CampaignDetailUiState(campaignId = 1, campaign = strahd)

        /** Party on screen and party on disk agree: nothing to save. */
        val savedParty = CampaignParticipantsUiState(
            participants = party,
            savedParticipants = party
        )

        val partyOfTwins = CampaignParticipantsUiState(
            participants = twins,
            savedParticipants = twins
        )

        val loadedSessions = CampaignSessionsUiState(sessions = listOf(deathHouse, barovia))
    }
}
