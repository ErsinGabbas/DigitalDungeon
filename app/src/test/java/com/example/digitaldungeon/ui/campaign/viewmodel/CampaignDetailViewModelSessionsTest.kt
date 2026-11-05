package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.session
import com.example.digitaldungeon.ui.campaign.state.PendingSessionDeletion
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
 * The session block of the campaign detail screen: the add form and the
 * confirmation-gated delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignDetailViewModelSessionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var campaignRepository: FakeCampaignRepository
    private lateinit var sessionRepository: FakeSessionRepository

    @Before
    fun setUp() {
        campaignRepository = FakeCampaignRepository()
        sessionRepository = FakeSessionRepository()
    }

    private fun TestScope.createViewModel(): CampaignDetailViewModel =
        CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle(
                mapOf(CampaignDetailViewModel.CAMPAIGN_ID_KEY to CAMPAIGN_ID)
            )
        ).also { advanceUntilIdle() }

    // ------------------------------------------------------------ add session

    @Test
    fun sessionsEmission_afterInit_updatesState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            assertTrue(viewModel.sessionUiState.value.sessions.isEmpty())

            val stored = listOf(session(id = 1, campaignId = CAMPAIGN_ID))
            sessionRepository.sessions.value = stored
            advanceUntilIdle()

            assertEquals(stored, viewModel.sessionUiState.value.sessions)
        }

    @Test
    fun addSession_withBlankTitle_setsErrorAndDoesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("   ")
            viewModel.onSessionNotesChange("notes are not a title")

            viewModel.addSession()
            advanceUntilIdle()

            assertEquals(
                "Session title cannot be empty",
                viewModel.sessionUiState.value.error
            )
            assertTrue(sessionRepository.addSessionCalls.isEmpty())
        }

    @Test
    fun addSession_passesCampaignIdTitleAndNotesToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("The Inn Burns Down")
            viewModel.onSessionNotesChange("again")

            viewModel.addSession()
            advanceUntilIdle()

            assertEquals(
                FakeSessionRepository.AddSessionCall(
                    campaignId = CAMPAIGN_ID,
                    title = "The Inn Burns Down",
                    notes = "again"
                ),
                sessionRepository.addSessionCalls.single()
            )
        }

    @Test
    fun addSession_onSuccess_clearsForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("The Inn Burns Down")
            viewModel.onSessionNotesChange("again")

            viewModel.addSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertEquals("", state.sessionTitle)
            assertEquals("", state.sessionNotes)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun addSession_whenRepositoryThrows_setsErrorAndKeepsForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.addSessionError = IllegalStateException("insert failed")

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("The Inn Burns Down")

            viewModel.addSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertEquals("insert failed", state.error)
            assertFalse(state.isLoading)
            assertEquals("The Inn Burns Down", state.sessionTitle)
        }

    // --------------------------------------------------------- delete session

    @Test
    fun requestDeleteSession_storesPendingTargetWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestDeleteSession(
                session(id = 3, campaignId = CAMPAIGN_ID, title = "Session Three")
            )
            advanceUntilIdle()

            assertEquals(
                PendingSessionDeletion(sessionId = 3, sessionTitle = "Session Three"),
                viewModel.sessionUiState.value.pendingDeletion
            )
            assertTrue(sessionRepository.deleteSessionCalls.isEmpty())
        }

    @Test
    fun confirmDeleteSession_deletesAndClearsPendingTarget() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteSession(session(id = 3, campaignId = CAMPAIGN_ID))

            viewModel.confirmDeleteSession()
            advanceUntilIdle()

            assertEquals(listOf(3), sessionRepository.deleteSessionCalls)
            assertNull(viewModel.sessionUiState.value.pendingDeletion)
            assertFalse(viewModel.sessionUiState.value.isLoading)
        }

    @Test
    fun cancelDeleteSession_clearsPendingTargetWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteSession(session(id = 3, campaignId = CAMPAIGN_ID))

            viewModel.cancelDeleteSession()
            advanceUntilIdle()

            assertNull(viewModel.sessionUiState.value.pendingDeletion)
            assertTrue(sessionRepository.deleteSessionCalls.isEmpty())
        }

    @Test
    fun confirmDeleteSession_withoutPendingTarget_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.confirmDeleteSession()
            advanceUntilIdle()

            assertTrue(sessionRepository.deleteSessionCalls.isEmpty())
            assertFalse(viewModel.sessionUiState.value.isLoading)
        }

    @Test
    fun confirmDeleteSession_whenRepositoryThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.deleteSessionError = IllegalStateException("delete failed")

            val viewModel = createViewModel()
            viewModel.requestDeleteSession(session(id = 3, campaignId = CAMPAIGN_ID))

            viewModel.confirmDeleteSession()
            advanceUntilIdle()

            assertEquals("delete failed", viewModel.sessionUiState.value.error)
            assertFalse(viewModel.sessionUiState.value.isLoading)
        }

    private companion object {
        const val CAMPAIGN_ID = 42
    }
}
