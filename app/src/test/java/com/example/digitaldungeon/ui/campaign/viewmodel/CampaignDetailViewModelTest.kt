package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.testing.session
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Campaign loading, navigation argument handling and - the important part - the
 * independence of the three error channels the screen exposes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var campaignRepository: FakeCampaignRepository
    private lateinit var sessionRepository: FakeSessionRepository

    @Before
    fun setUp() {
        campaignRepository = FakeCampaignRepository()
        sessionRepository = FakeSessionRepository()
    }

    /** Creates the ViewModel and lets the `init` subscriptions settle. */
    private fun TestScope.createViewModel(campaignId: Int = CAMPAIGN_ID): CampaignDetailViewModel =
        CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle(
                mapOf(CampaignDetailViewModel.CAMPAIGN_ID_KEY to campaignId)
            )
        ).also { advanceUntilIdle() }

    // ------------------------------------------------- navigation argument

    @Test
    fun init_readsCampaignIdFromSavedStateHandle() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(campaignId = 42)

            assertEquals(42, viewModel.campaignUiState.value.campaignId)
            assertEquals(listOf(42), campaignRepository.getCampaignByIdFlowCalls)
            assertEquals(listOf(42), sessionRepository.getSessionsForCampaignCalls)
        }

    @Test(expected = IllegalStateException::class)
    fun init_withoutCampaignIdArgument_fails() {
        // A missing navigation argument is a programming error, not campaign number 0.
        CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    // ------------------------------------------------------ campaign loading

    @Test
    fun init_loadsCampaignIntoStateAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = campaign(id = CAMPAIGN_ID, name = "Dragon Heist")
            campaignRepository.campaignById.value = stored

            val viewModel = createViewModel()

            assertEquals(stored, viewModel.campaignUiState.value.campaign)
            assertFalse(viewModel.campaignUiState.value.isLoading)
            assertNull(viewModel.campaignUiState.value.error)
        }

    @Test
    fun init_loadsSessionsIntoStateAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(session(id = 1, campaignId = CAMPAIGN_ID))
            sessionRepository.sessions.value = stored

            val viewModel = createViewModel()

            assertEquals(stored, viewModel.sessionUiState.value.sessions)
            assertFalse(viewModel.sessionUiState.value.isLoading)
            assertNull(viewModel.sessionUiState.value.error)
        }

    @Test
    fun missingCampaign_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            // The flow emits null straight away: the campaign is not in the database.
            campaignRepository.campaignById.value = null

            val viewModel = createViewModel()

            assertEquals("Campaign not found", viewModel.campaignUiState.value.error)
            assertFalse(viewModel.campaignUiState.value.isLoading)
        }

    @Test
    fun campaignAppearingAfterMissing_clearsNotFoundError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            assertEquals("Campaign not found", viewModel.campaignUiState.value.error)

            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID)
            advanceUntilIdle()

            assertNull(viewModel.campaignUiState.value.error)
        }

    @Test
    fun campaignFlowFailure_setsCampaignErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.getCampaignByIdFlowError = IllegalStateException("campaign query failed")

            val viewModel = createViewModel()

            assertEquals("campaign query failed", viewModel.campaignUiState.value.error)
            assertFalse(viewModel.campaignUiState.value.isLoading)
        }

    @Test
    fun sessionsFlowFailure_setsSessionErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.getSessionsForCampaignError = IllegalStateException("session query failed")

            val viewModel = createViewModel()

            assertEquals("session query failed", viewModel.sessionUiState.value.error)
            assertFalse(viewModel.sessionUiState.value.isLoading)
        }

    // ------------------------------------------- independent error channels

    @Test
    fun sessionsFlowFailure_doesNotLeakIntoCampaignOrParticipantState() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.getSessionsForCampaignError = IllegalStateException("session query failed")
            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID)

            val viewModel = createViewModel()

            assertEquals("session query failed", viewModel.sessionUiState.value.error)
            assertNull(viewModel.campaignUiState.value.error)
            assertNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun campaignFlowFailure_doesNotLeakIntoSessionState() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.getCampaignByIdFlowError = IllegalStateException("campaign query failed")
            sessionRepository.sessions.value = listOf(session(id = 1, campaignId = CAMPAIGN_ID))

            val viewModel = createViewModel()

            assertEquals("campaign query failed", viewModel.campaignUiState.value.error)
            assertNull(viewModel.sessionUiState.value.error)
            assertEquals(1, viewModel.sessionUiState.value.sessions.size)
        }

    /**
     * Regression guard: the three blocks used to share one error field, so any session
     * emission wiped the "could not save participants" message off the screen.
     */
    @Test
    fun participantsSaveFailure_isNotClearedBySessionsEmission() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.updateCampaignParticipantsError = IllegalStateException("save failed")

            val viewModel = createViewModel()
            viewModel.saveParticipants()
            advanceUntilIdle()
            assertEquals("save failed", viewModel.participantsUiState.value.error)

            sessionRepository.sessions.value = listOf(session(id = 5, campaignId = CAMPAIGN_ID))
            advanceUntilIdle()

            assertEquals("save failed", viewModel.participantsUiState.value.error)
        }

    @Test
    fun participantsSaveFailure_isNotClearedByCampaignEmission() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.updateCampaignParticipantsError = IllegalStateException("save failed")

            val viewModel = createViewModel()
            viewModel.saveParticipants()
            advanceUntilIdle()

            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID, name = "renamed")
            advanceUntilIdle()

            assertEquals("save failed", viewModel.participantsUiState.value.error)
        }

    @Test
    fun participantsSaveFailure_doesNotLeakIntoSessionOrCampaignState() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.updateCampaignParticipantsError = IllegalStateException("save failed")
            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID)

            val viewModel = createViewModel()
            viewModel.saveParticipants()
            advanceUntilIdle()

            assertEquals("save failed", viewModel.participantsUiState.value.error)
            assertNull(viewModel.sessionUiState.value.error)
            assertNull(viewModel.campaignUiState.value.error)
        }

    @Test
    fun sessionsFlowFailure_isNotClearedBySuccessfulParticipantsSave() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.getSessionsForCampaignError = IllegalStateException("session query failed")

            val viewModel = createViewModel()
            assertEquals("session query failed", viewModel.sessionUiState.value.error)

            viewModel.saveParticipants()
            advanceUntilIdle()

            assertEquals("session query failed", viewModel.sessionUiState.value.error)
            assertNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun sessionAddFailure_isNotClearedByCampaignEmission() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.addSessionError = IllegalStateException("add session failed")

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("Session 1")
            viewModel.addSession()
            advanceUntilIdle()
            assertEquals("add session failed", viewModel.sessionUiState.value.error)

            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID, name = "renamed")
            advanceUntilIdle()

            assertEquals("add session failed", viewModel.sessionUiState.value.error)
            assertTrue(viewModel.campaignUiState.value.error == null)
        }

    private companion object {
        const val CAMPAIGN_ID = 42
    }
}
