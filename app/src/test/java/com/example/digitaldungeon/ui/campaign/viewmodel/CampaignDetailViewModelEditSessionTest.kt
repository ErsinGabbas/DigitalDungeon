package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
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
 * Editing a session from the campaign detail screen - the same contract the campaign
 * edit dialog has: refusals stay inside the dialog, and the dialog is never on screen
 * next to the delete confirmation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignDetailViewModelEditSessionTest {

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

    private val target = session(
        id = 3,
        campaignId = CAMPAIGN_ID,
        title = "The Inn Burns Down",
        notes = "again"
    )

    // ------------------------------------------------------- opening the dialog

    @Test
    fun requestEditSession_fillsEditFieldsFromSession() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestEditSession(target)

            val state = viewModel.sessionUiState.value
            assertEquals(target, state.editingSession)
            assertEquals("The Inn Burns Down", state.editSessionTitle)
            assertEquals("again", state.editSessionNotes)
            assertNull(state.editError)
        }

    @Test
    fun requestEditSession_doesNotTouchTheRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestEditSession(target)
            advanceUntilIdle()

            assertTrue(sessionRepository.updateSessionCalls.isEmpty())
        }

    // --------------------------------------------- mutually exclusive dialogs

    @Test
    fun requestEditSession_clearsPendingDeletion() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteSession(session(id = 99, campaignId = CAMPAIGN_ID))

            viewModel.requestEditSession(target)

            assertNull(viewModel.sessionUiState.value.pendingDeletion)
            assertEquals(target, viewModel.sessionUiState.value.editingSession)
        }

    @Test
    fun requestDeleteSession_closesTheEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("half typed title")

            viewModel.requestDeleteSession(
                session(id = 99, campaignId = CAMPAIGN_ID, title = "doomed")
            )

            val state = viewModel.sessionUiState.value
            assertNull(state.editingSession)
            assertEquals("", state.editSessionTitle)
            assertEquals("", state.editSessionNotes)
            assertNull(state.editError)
            assertEquals(
                PendingSessionDeletion(sessionId = 99, sessionTitle = "doomed"),
                state.pendingDeletion
            )
        }

    // ------------------------------------------------------------- validation

    @Test
    fun confirmEditSession_withBlankTitle_setsEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("   ")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals(
                CampaignDetailViewModel.SESSION_TITLE_REQUIRED,
                viewModel.sessionUiState.value.editError
            )
        }

    @Test
    fun confirmEditSession_withBlankTitle_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertTrue(sessionRepository.updateSessionCalls.isEmpty())
        }

    @Test
    fun confirmEditSession_withBlankTitle_keepsTheDialogOpen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("   ")
            viewModel.onEditSessionNotesChange("notes survive the refusal")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertEquals(target, state.editingSession)
            assertEquals("   ", state.editSessionTitle)
            assertEquals("notes survive the refusal", state.editSessionNotes)
        }

    @Test
    fun confirmEditSession_withBlankTitle_reportsInsideTheDialogNotOnTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            // A message behind the dialog would never be read.
            assertNull(viewModel.sessionUiState.value.error)
        }

    @Test
    fun onEditSessionTitleChange_clearsAPreviousEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("")
            viewModel.confirmEditSession()
            advanceUntilIdle()
            assertEquals(
                CampaignDetailViewModel.SESSION_TITLE_REQUIRED,
                viewModel.sessionUiState.value.editError
            )

            viewModel.onEditSessionTitleChange("T")

            assertNull(viewModel.sessionUiState.value.editError)
            assertEquals("T", viewModel.sessionUiState.value.editSessionTitle)
        }

    @Test
    fun onEditSessionNotesChange_clearsAPreviousEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("")
            viewModel.confirmEditSession()
            advanceUntilIdle()

            viewModel.onEditSessionNotesChange("new notes")

            assertNull(viewModel.sessionUiState.value.editError)
        }

    // ----------------------------------------------------------------- saving

    @Test
    fun confirmEditSession_sendsEditedValuesToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("The Inn Burns Down Again")
            viewModel.onEditSessionNotesChange("third time this campaign")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals(
                FakeSessionRepository.UpdateSessionCall(
                    id = 3,
                    title = "The Inn Burns Down Again",
                    notes = "third time this campaign"
                ),
                sessionRepository.updateSessionCalls.single()
            )
        }

    @Test
    fun confirmEditSession_trimsTitleAndNotes() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("   Night Three   ")
            viewModel.onEditSessionNotesChange("\n  nobody slept \n")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals(
                FakeSessionRepository.UpdateSessionCall(
                    id = 3,
                    title = "Night Three",
                    notes = "nobody slept"
                ),
                sessionRepository.updateSessionCalls.single()
            )
        }

    @Test
    fun confirmEditSession_allowsEmptyNotes() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionNotesChange("    ")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals("", sessionRepository.updateSessionCalls.single().notes)
        }

    @Test
    fun confirmEditSession_onSuccess_closesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("Night Three")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertNull(state.editingSession)
            assertEquals("", state.editSessionTitle)
            assertEquals("", state.editSessionNotes)
            assertNull(state.editError)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun confirmEditSession_doesNotTouchTheAddSessionForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onSessionTitleChange("a session being typed")
            viewModel.onSessionNotesChange("with notes")
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("Night Three")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertEquals("a session being typed", state.sessionTitle)
            assertEquals("with notes", state.sessionNotes)
        }

    @Test
    fun confirmEditSession_withoutAnOpenDialog_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertTrue(sessionRepository.updateSessionCalls.isEmpty())
            assertNull(viewModel.sessionUiState.value.editError)
        }

    @Test
    fun confirmEditSession_whenRepositoryThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.updateSessionError = IllegalStateException("update failed")

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("Night Three")

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals("update failed", viewModel.sessionUiState.value.error)
            assertFalse(viewModel.sessionUiState.value.isLoading)
        }

    @Test
    fun confirmEditSession_whenRepositoryThrows_doesNotLeakIntoCampaignState() =
        runTest(mainDispatcherRule.testDispatcher) {

            sessionRepository.updateSessionError = IllegalStateException("update failed")
            campaignRepository.campaignById.value = campaign(id = CAMPAIGN_ID)

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertEquals("update failed", viewModel.sessionUiState.value.error)
            assertNull(viewModel.campaignUiState.value.error)
            assertNull(viewModel.participantsUiState.value.error)
        }

    // ------------------------------------------------------------- cancelling

    @Test
    fun cancelEditSession_closesTheDialogWithoutCallingRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("Night Three")

            viewModel.cancelEditSession()
            advanceUntilIdle()

            val state = viewModel.sessionUiState.value
            assertNull(state.editingSession)
            assertEquals("", state.editSessionTitle)
            assertEquals("", state.editSessionNotes)
            assertTrue(sessionRepository.updateSessionCalls.isEmpty())
        }

    @Test
    fun confirmEditSession_afterCancel_doesNotUpdate() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("Night Three")
            viewModel.cancelEditSession()

            viewModel.confirmEditSession()
            advanceUntilIdle()

            assertTrue(sessionRepository.updateSessionCalls.isEmpty())
        }

    @Test
    fun cancelEditSession_clearsALeftoverEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditSession(target)
            viewModel.onEditSessionTitleChange("")
            viewModel.confirmEditSession()
            advanceUntilIdle()

            viewModel.cancelEditSession()

            assertNull(viewModel.sessionUiState.value.editError)
        }

    private companion object {
        const val CAMPAIGN_ID = 42
    }
}
