package com.example.digitaldungeon.ui.campaign.viewmodel

import com.example.digitaldungeon.R
import com.example.digitaldungeon.testing.FakeBackupService
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
import com.example.digitaldungeon.ui.text.UiText
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
 * Renaming a campaign through the edit dialog.
 *
 * The dialog covers the screen, so the two rules worth guarding are "a refusal is
 * reported inside the dialog and keeps it open" and "the edit dialog and the delete
 * confirmation are never on screen together".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignViewModelEditTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeCampaignRepository
    private lateinit var backupService: FakeBackupService

    @Before
    fun setUp() {
        repository = FakeCampaignRepository()
        backupService = FakeBackupService()
    }

    private fun TestScope.createViewModel(): CampaignViewModel =
        CampaignViewModel(repository, backupService).also { advanceUntilIdle() }

    private val target = campaign(
        id = 11,
        name = "Curse of Strahd",
        description = "gothic horror in Barovia"
    )

    // ------------------------------------------------------- opening the dialog

    @Test
    fun requestEditCampaign_fillsEditFieldsFromCampaign() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestEditCampaign(target)

            val state = viewModel.uiState.value
            assertEquals(target, state.editingCampaign)
            assertEquals("Curse of Strahd", state.editName)
            assertEquals("gothic horror in Barovia", state.editDescription)
            assertNull(state.editError)
        }

    @Test
    fun requestEditCampaign_doesNotTouchTheRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.requestEditCampaign(target)
            advanceUntilIdle()

            assertTrue(repository.updateCampaignCalls.isEmpty())
        }

    // ------------------------------------------- mutually exclusive dialogs

    @Test
    fun requestEditCampaign_clearsPendingDeletion() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 99, name = "doomed"))

            viewModel.requestEditCampaign(target)

            assertNull(viewModel.uiState.value.pendingDeletion)
            assertEquals(target, viewModel.uiState.value.editingCampaign)
        }

    @Test
    fun requestDeleteCampaign_closesTheEditDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("half typed name")

            viewModel.requestDeleteCampaign(campaign(id = 99, name = "doomed"))

            val state = viewModel.uiState.value
            assertNull(state.editingCampaign)
            assertEquals("", state.editName)
            assertEquals("", state.editDescription)
            assertNull(state.editError)
            assertEquals(
                PendingCampaignDeletion(campaignId = 99, campaignName = "doomed"),
                state.pendingDeletion
            )
        }

    // ------------------------------------------------------------- validation

    @Test
    fun confirmEditCampaign_withBlankName_setsEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("   ")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertEquals(
                UiText.of(R.string.campaign_name_required),
                viewModel.uiState.value.editError
            )
        }

    @Test
    fun confirmEditCampaign_withBlankName_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertTrue(repository.updateCampaignCalls.isEmpty())
        }

    @Test
    fun confirmEditCampaign_withBlankName_keepsTheDialogOpen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("   ")
            viewModel.onEditDescriptionChange("a description survives the refusal")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(target, state.editingCampaign)
            assertEquals("   ", state.editName)
            assertEquals("a description survives the refusal", state.editDescription)
        }

    @Test
    fun confirmEditCampaign_withBlankName_reportsInsideTheDialogNotOnTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            // A message behind the dialog would never be read.
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun onEditNameChange_clearsAPreviousEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("")
            viewModel.confirmEditCampaign()
            advanceUntilIdle()
            assertEquals(
                UiText.of(R.string.campaign_name_required),
                viewModel.uiState.value.editError
            )

            viewModel.onEditNameChange("R")

            assertNull(viewModel.uiState.value.editError)
            assertEquals("R", viewModel.uiState.value.editName)
        }

    @Test
    fun onEditDescriptionChange_clearsAPreviousEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("")
            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            viewModel.onEditDescriptionChange("a new description")

            assertNull(viewModel.uiState.value.editError)
        }

    // ------------------------------------------------------------- saving

    @Test
    fun confirmEditCampaign_sendsEditedValuesToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("Curse of Strahd Reloaded")
            viewModel.onEditDescriptionChange("still gothic, still horror")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertEquals(
                FakeCampaignRepository.UpdateCampaignCall(
                    id = 11,
                    name = "Curse of Strahd Reloaded",
                    description = "still gothic, still horror"
                ),
                repository.updateCampaignCalls.single()
            )
        }

    @Test
    fun confirmEditCampaign_trimsNameAndDescription() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("   Barovia   ")
            viewModel.onEditDescriptionChange("\n  mist everywhere \n")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertEquals(
                FakeCampaignRepository.UpdateCampaignCall(
                    id = 11,
                    name = "Barovia",
                    description = "mist everywhere"
                ),
                repository.updateCampaignCalls.single()
            )
        }

    @Test
    fun confirmEditCampaign_allowsAnEmptyDescription() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditDescriptionChange("    ")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertEquals("", repository.updateCampaignCalls.single().description)
        }

    @Test
    fun confirmEditCampaign_onSuccess_closesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("Barovia")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingCampaign)
            assertEquals("", state.editName)
            assertEquals("", state.editDescription)
            assertNull(state.editError)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun confirmEditCampaign_withoutAnOpenDialog_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertTrue(repository.updateCampaignCalls.isEmpty())
            assertNull(viewModel.uiState.value.editError)
        }

    @Test
    fun confirmEditCampaign_whenRepositoryThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.updateCampaignError = IllegalStateException("update failed")

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("Barovia")

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertEquals(UiText.Raw("update failed"), viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // ------------------------------------------------------------ cancelling

    @Test
    fun cancelEditCampaign_closesTheDialogWithoutCallingRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("Barovia")

            viewModel.cancelEditCampaign()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.editingCampaign)
            assertEquals("", state.editName)
            assertEquals("", state.editDescription)
            assertTrue(repository.updateCampaignCalls.isEmpty())
        }

    @Test
    fun confirmEditCampaign_afterCancel_doesNotUpdate() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("Barovia")
            viewModel.cancelEditCampaign()

            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            assertTrue(repository.updateCampaignCalls.isEmpty())
        }

    @Test
    fun cancelEditCampaign_clearsALeftoverEditError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestEditCampaign(target)
            viewModel.onEditNameChange("")
            viewModel.confirmEditCampaign()
            advanceUntilIdle()

            viewModel.cancelEditCampaign()

            assertNull(viewModel.uiState.value.editError)
        }
}
