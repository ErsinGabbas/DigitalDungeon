package com.example.digitaldungeon.ui.session.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEventType
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionEventRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.testing.participant
import com.example.digitaldungeon.testing.session
import com.example.digitaldungeon.testing.sessionEvent
import com.example.digitaldungeon.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Naming who was present at an event.
 *
 * Two lists that look alike and must never be confused: the campaign roster, which is
 * live and only feeds the pickers, and the cast stored on each event, which is a
 * snapshot taken when it happened. The rules worth guarding all follow from that -
 * editing the text of an event must hand its own snapshot back untouched, and someone
 * who has since left the campaign must stay visible in the old events that name them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelParticipantsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var eventRepository: FakeSessionEventRepository
    private lateinit var campaignRepository: FakeCampaignRepository

    @Before
    fun setUp() {
        sessionRepository = FakeSessionRepository()
        eventRepository = FakeSessionEventRepository()
        campaignRepository = FakeCampaignRepository()
    }

    private val aragorn = participant(name = "Aragorn", level = 4, maxHp = 30)
    private val legolas = participant(name = "Legolas", level = 6, maxHp = 28)
    private val gimli = participant(name = "Gimli", level = 5, maxHp = 35)

    /**
     * Creates the ViewModel on a session that belongs to [CAMPAIGN_ID], with [roster]
     * as that campaign's current party, and lets the `init` subscriptions settle.
     */
    private fun TestScope.createViewModel(
        roster: List<EventParticipant> = emptyList()
    ): SessionDetailViewModel {

        sessionRepository.session.value = session(id = SESSION_ID, campaignId = CAMPAIGN_ID)
        campaignRepository.campaignById.value =
            campaign(id = CAMPAIGN_ID, participants = roster)

        return newViewModel()
    }

    private fun TestScope.newViewModel(): SessionDetailViewModel =
        SessionDetailViewModel(
            sessionRepository = sessionRepository,
            sessionEventRepository = eventRepository,
            campaignRepository = campaignRepository,
            savedStateHandle = SavedStateHandle(mapOf(SESSION_ID_KEY to SESSION_ID))
        ).also { advanceUntilIdle() }

    /** Replaces the stored roster of the campaign the session belongs to. */
    private fun publishRoster(roster: List<EventParticipant>) {
        campaignRepository.campaignById.value =
            campaign(id = CAMPAIGN_ID, participants = roster)
    }

    // ------------------------------------------------------- the campaign roster

    /**
     * The roster hangs off the campaign, which is only reachable through the session -
     * so it cannot be requested in `init` and has to wait for the first emission.
     */
    @Test
    fun init_followsTheCampaignOfTheLoadedSession() =
        runTest(mainDispatcherRule.testDispatcher) {

            createViewModel(roster = listOf(aragorn))

            assertEquals(listOf(CAMPAIGN_ID), campaignRepository.getCampaignByIdFlowCalls)
        }

    @Test
    fun init_putsTheCampaignRosterIntoState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))

            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.campaignParticipants
            )
        }

    @Test
    fun aSessionThatNamesNoCampaign_leavesTheRosterEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {

            // A deleted session names no campaign, so there is no roster to ask for.
            sessionRepository.session.value = null
            campaignRepository.campaignById.value =
                campaign(id = CAMPAIGN_ID, participants = listOf(aragorn))

            val viewModel = newViewModel()

            assertTrue(viewModel.uiState.value.campaignParticipants.isEmpty())
            assertTrue(campaignRepository.getCampaignByIdFlowCalls.isEmpty())
        }

    @Test
    fun rosterEmission_replacesTheOfferedParticipants() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))

            publishRoster(listOf(aragorn, gimli))
            advanceUntilIdle()

            assertEquals(
                listOf(aragorn, gimli),
                viewModel.uiState.value.campaignParticipants
            )
        }

    /**
     * An ordinary session edit makes `getSessionById` emit again with the same campaign;
     * restarting the roster query for that would re-subscribe on every rename.
     */
    @Test
    fun aSessionEditThatKeepsTheCampaign_doesNotRestartTheRosterQuery() =
        runTest(mainDispatcherRule.testDispatcher) {

            createViewModel(roster = listOf(aragorn))

            sessionRepository.session.value =
                session(id = SESSION_ID, campaignId = CAMPAIGN_ID, title = "renamed")
            advanceUntilIdle()

            assertEquals(listOf(CAMPAIGN_ID), campaignRepository.getCampaignByIdFlowCalls)
        }

    @Test
    fun aSessionThatNamesAnotherCampaign_followsTheNewRoster() =
        runTest(mainDispatcherRule.testDispatcher) {

            createViewModel(roster = listOf(aragorn))

            sessionRepository.session.value =
                session(id = SESSION_ID, campaignId = OTHER_CAMPAIGN_ID)
            advanceUntilIdle()

            assertEquals(
                listOf(CAMPAIGN_ID, OTHER_CAMPAIGN_ID),
                campaignRepository.getCampaignByIdFlowCalls
            )
        }

    @Test
    fun aFailingRosterQuery_isReportedAsAnError() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.getCampaignByIdFlowError =
                IllegalStateException("roster query failed")
            sessionRepository.session.value = session(id = SESSION_ID, campaignId = CAMPAIGN_ID)

            val viewModel = newViewModel()

            assertEquals(UiText.Raw("roster query failed"), viewModel.uiState.value.error)
        }

    // ------------------------------------------------------ picker visibility

    @Test
    fun aCampaignWithoutParticipants_hidesTheQuickNotePicker() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = emptyList())

            // An empty picker is worse than no picker at all.
            assertFalse(viewModel.uiState.value.canSelectQuickNoteParticipants)
        }

    @Test
    fun aCampaignWithParticipants_showsTheQuickNotePicker() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))

            assertTrue(viewModel.uiState.value.canSelectQuickNoteParticipants)
        }

    // ------------------------------------------------- quick note selection

    @Test
    fun onQuickNoteParticipantToggled_selectsTheParticipant() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))

            viewModel.onQuickNoteParticipantToggled(legolas)

            assertEquals(listOf(legolas), viewModel.uiState.value.quickNoteParticipants)
        }

    @Test
    fun onQuickNoteParticipantToggled_twice_deselectsTheParticipant() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onQuickNoteParticipantToggled(legolas)

            viewModel.onQuickNoteParticipantToggled(legolas)

            // "Nobody selected" is the usual state of a quick note and has to be reachable.
            assertTrue(viewModel.uiState.value.quickNoteParticipants.isEmpty())
        }

    @Test
    fun onQuickNoteParticipantToggled_keepsTheParticipantsAlreadySelected() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas, gimli))

            viewModel.onQuickNoteParticipantToggled(aragorn)
            viewModel.onQuickNoteParticipantToggled(gimli)

            assertEquals(
                listOf(aragorn, gimli),
                viewModel.uiState.value.quickNoteParticipants
            )
        }

    @Test
    fun deselectingOneParticipant_leavesTheOthersSelected() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas, gimli))
            viewModel.onQuickNoteParticipantToggled(aragorn)
            viewModel.onQuickNoteParticipantToggled(legolas)
            viewModel.onQuickNoteParticipantToggled(gimli)

            viewModel.onQuickNoteParticipantToggled(legolas)

            assertEquals(
                listOf(aragorn, gimli),
                viewModel.uiState.value.quickNoteParticipants
            )
        }

    @Test
    fun onAddQuickNote_copiesTheSelectedCastIntoTheEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEventTypeSelected(SessionEventType.COMBAT)
            viewModel.onQuickNoteTextChanged("the goblin rolls a natural 20")
            viewModel.onQuickNoteParticipantToggled(aragorn)
            viewModel.onQuickNoteParticipantToggled(legolas)

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertEquals(
                FakeSessionEventRepository.AddEventCall(
                    sessionId = SESSION_ID,
                    type = SessionEventType.COMBAT,
                    title = null,
                    content = "the goblin rolls a natural 20",
                    participants = listOf(aragorn, legolas)
                ),
                eventRepository.addEventCalls.single()
            )
        }

    @Test
    fun onAddQuickNote_withoutASelection_storesAnEmptyCast() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onQuickNoteTextChanged("it rains")

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            // A note without a cast is the normal case, not an incomplete one.
            assertTrue(eventRepository.addEventCalls.single().participants.isEmpty())
        }

    /**
     * Carrying the cast over to the next note would quietly attribute an unrelated
     * entry to the same people; naming participants stays a deliberate act.
     */
    @Test
    fun onAddQuickNote_onSuccess_clearsTheSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onQuickNoteTextChanged("it rains")
            viewModel.onQuickNoteParticipantToggled(aragorn)

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.quickNoteParticipants.isEmpty())
        }

    @Test
    fun onAddQuickNote_whenTheRepositoryThrows_keepsTheSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            eventRepository.addEventError = IllegalStateException("insert failed")

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onQuickNoteTextChanged("it rains")
            viewModel.onQuickNoteParticipantToggled(aragorn)

            viewModel.onAddQuickNote()
            advanceUntilIdle()

            // The note is still on screen, so its cast has to be too.
            assertEquals(listOf(aragorn), viewModel.uiState.value.quickNoteParticipants)
        }

    /**
     * A pending pick must not outlive the person it names: the chip would vanish with
     * the roster while the choice silently stayed, and the note would be saved for
     * someone the user can no longer see.
     */
    @Test
    fun aParticipantRemovedFromTheCampaign_dropsOutOfTheQuickNoteSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onQuickNoteParticipantToggled(aragorn)
            viewModel.onQuickNoteParticipantToggled(legolas)

            publishRoster(listOf(aragorn))
            advanceUntilIdle()

            assertEquals(listOf(aragorn), viewModel.uiState.value.quickNoteParticipants)
        }

    // --------------------------------------------------- the edit dialog's cast

    @Test
    fun onEditEvent_seedsTheSelectionFromTheEvent() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))

            viewModel.onEditEvent(
                sessionEvent(id = 4, participants = listOf(legolas))
            )

            assertEquals(listOf(legolas), viewModel.uiState.value.editParticipants)
        }

    @Test
    fun onEditEvent_forAnEventWithoutACast_startsWithAnEmptySelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))

            viewModel.onEditEvent(sessionEvent(id = 4, participants = emptyList()))

            assertTrue(viewModel.uiState.value.editParticipants.isEmpty())
        }

    /**
     * Regression guard: `updateEvent` replaces the stored snapshot with whatever it is
     * handed, so a dialog that only changed the text has to hand the old cast back.
     * Leaving the argument out is what used to erase the participants of an event the
     * moment a typo in it was fixed.
     */
    @Test
    fun onUpdateEvent_afterATextOnlyEdit_writesTheEventsOwnCastBack() =
        runTest(mainDispatcherRule.testDispatcher) {

            val cast = listOf(aragorn, legolas)
            val viewModel = createViewModel(roster = cast)
            viewModel.onEditEvent(
                sessionEvent(id = 4, title = "Goblin Ambush", content = "tree goblins", participants = cast)
            )

            viewModel.onEditContentChange("three goblins")
            viewModel.onUpdateEvent(eventId = 4)
            advanceUntilIdle()

            assertEquals(cast, eventRepository.updateEventCalls.single().participants)
        }

    @Test
    fun onEditParticipantToggled_addsSomeoneToTheSnapshot() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))

            viewModel.onEditParticipantToggled(legolas)

            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.editParticipants
            )
        }

    @Test
    fun onEditParticipantToggled_removesSomeoneFromTheSnapshot() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(
                sessionEvent(id = 4, participants = listOf(aragorn, legolas))
            )

            viewModel.onEditParticipantToggled(aragorn)

            assertEquals(listOf(legolas), viewModel.uiState.value.editParticipants)
        }

    @Test
    fun onUpdateEvent_writesTheEditedSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas, gimli))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))
            viewModel.onEditParticipantToggled(gimli)

            viewModel.onUpdateEvent(eventId = 4)
            advanceUntilIdle()

            assertEquals(
                listOf(aragorn, gimli),
                eventRepository.updateEventCalls.single().participants
            )
        }

    @Test
    fun onUpdateEvent_afterClearingTheSelection_writesAnEmptyCast() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))
            viewModel.onEditParticipantToggled(aragorn)

            viewModel.onUpdateEvent(eventId = 4)
            advanceUntilIdle()

            // Unchecking everybody is a statement, not a missing value.
            assertTrue(eventRepository.updateEventCalls.single().participants.isEmpty())
        }

    @Test
    fun onUpdateEvent_onSuccess_clearsTheEditSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))

            viewModel.onUpdateEvent(eventId = 4)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.editParticipants.isEmpty())
        }

    @Test
    fun onEditDialogDismiss_clearsTheEditSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))

            viewModel.onEditDialogDismiss()

            assertTrue(viewModel.uiState.value.editParticipants.isEmpty())
        }

    @Test
    fun onDeleteEventClick_clearsTheEditSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))

            viewModel.onDeleteEventClick(sessionEvent(id = 5))

            assertTrue(viewModel.uiState.value.editParticipants.isEmpty())
        }

    // --------------------------------------------- what the edit dialog offers

    @Test
    fun editParticipantOptions_offerTheCampaignRoster() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(aragorn)))

            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.editParticipantOptions
            )
        }

    @Test
    fun editParticipantOptions_listNobodyTwice() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))

            // Everyone in the cast is on the roster as well.
            viewModel.onEditEvent(
                sessionEvent(id = 4, participants = listOf(aragorn, legolas))
            )

            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.editParticipantOptions
            )
        }

    /**
     * An event keeps the cast it was recorded with, so pruning the selection against
     * the live roster would erase exactly what the snapshot exists to preserve - and
     * the user could neither see nor deliberately drop the person who left.
     */
    @Test
    fun aParticipantRemovedFromTheCampaign_staysInTheEditSelection() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(
                sessionEvent(id = 4, participants = listOf(aragorn, legolas))
            )

            publishRoster(listOf(aragorn))
            advanceUntilIdle()

            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.editParticipants
            )
        }

    @Test
    fun aParticipantRemovedFromTheCampaign_isStillOfferedByTheEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(legolas)))

            publishRoster(listOf(aragorn))
            advanceUntilIdle()

            // The roster first, then whoever the event names and the campaign no longer does.
            assertEquals(
                listOf(aragorn, legolas),
                viewModel.uiState.value.editParticipantOptions
            )
        }

    @Test
    fun aTextOnlyEditAfterAParticipantLeftTheCampaign_stillWritesThemBack() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = listOf(aragorn, legolas))
            viewModel.onEditEvent(
                sessionEvent(id = 4, content = "tree goblins", participants = listOf(legolas))
            )
            publishRoster(listOf(aragorn))
            advanceUntilIdle()

            viewModel.onEditContentChange("three goblins")
            viewModel.onUpdateEvent(eventId = 4)
            advanceUntilIdle()

            // Fixing a typo must not rewrite history.
            assertEquals(
                listOf(legolas),
                eventRepository.updateEventCalls.single().participants
            )
        }

    @Test
    fun withoutARosterAndWithoutACast_theEditPickerIsHidden() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = emptyList())

            viewModel.onEditEvent(sessionEvent(id = 4, participants = emptyList()))

            assertFalse(viewModel.uiState.value.canSelectEditParticipants)
        }

    @Test
    fun anEventWhoseWholeCastLeftTheCampaign_stillShowsTheEditPicker() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(roster = emptyList())

            viewModel.onEditEvent(sessionEvent(id = 4, participants = listOf(legolas)))

            // Otherwise the only thing the dialog could do with legolas is lose him.
            assertTrue(viewModel.uiState.value.canSelectEditParticipants)
        }

    private companion object {
        const val SESSION_ID = 7
        const val SESSION_ID_KEY = "sessionId"
        const val CAMPAIGN_ID = 42
        const val OTHER_CAMPAIGN_ID = 43
    }
}
