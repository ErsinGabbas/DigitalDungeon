package com.example.digitaldungeon.ui.campaign.viewmodel

import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.ui.campaign.state.PendingCampaignDeletion
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

@OptIn(ExperimentalCoroutinesApi::class)
class CampaignViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeCampaignRepository

    @Before
    fun setUp() {
        repository = FakeCampaignRepository()
    }

    /** Creates the ViewModel and lets its `init` subscription settle. */
    private fun TestScope.createViewModel(): CampaignViewModel =
        CampaignViewModel(repository).also { advanceUntilIdle() }

    // ---------------------------------------------------------------- loading

    @Test
    fun init_collectsCampaignsAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(campaign(id = 1), campaign(id = 2))
            repository.campaigns.value = stored

            val viewModel = createViewModel()

            assertEquals(stored, viewModel.uiState.value.campaigns)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun campaignsEmission_afterInit_updatesState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            assertTrue(viewModel.uiState.value.campaigns.isEmpty())

            val stored = listOf(campaign(id = 7, name = "Curse of Strahd"))
            repository.campaigns.value = stored
            advanceUntilIdle()

            assertEquals(stored, viewModel.uiState.value.campaigns)
        }

    @Test
    fun getCampaigns_whenFlowThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.getCampaignsError = IllegalStateException("database is on fire")

            val viewModel = createViewModel()

            assertEquals("database is on fire", viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // ------------------------------------------------------------ add campaign

    @Test
    fun addCampaign_withBlankName_doesNotCallRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("   ")
            viewModel.onCampaignDescriptionChange("a description is not enough")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertTrue(repository.addCampaignCalls.isEmpty())
        }

    @Test
    fun addCampaign_withBlankName_keepsTypedDescription() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("")
            viewModel.onCampaignDescriptionChange("a description is not enough")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertEquals(
                "a description is not enough",
                viewModel.uiState.value.campaignDescription
            )
        }

    @Test
    fun addCampaign_passesNameAndDescriptionToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("Tomb of Annihilation")
            viewModel.onCampaignDescriptionChange("jungle, death, more death")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertEquals(1, repository.addCampaignCalls.size)
            assertEquals(
                FakeCampaignRepository.AddCampaignCall(
                    name = "Tomb of Annihilation",
                    description = "jungle, death, more death",
                    participants = emptyList()
                ),
                repository.addCampaignCalls.single()
            )
        }

    @Test
    fun addCampaign_onSuccess_clearsInputFields() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("Tomb of Annihilation")
            viewModel.onCampaignDescriptionChange("jungle, death, more death")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.campaignName)
            assertEquals("", viewModel.uiState.value.campaignDescription)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun addCampaign_whenRepositoryThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.addCampaignError = IllegalStateException("insert failed")

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("Tomb of Annihilation")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertEquals("insert failed", viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun addCampaign_whenRepositoryThrows_keepsInputFields() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.addCampaignError = IllegalStateException("insert failed")

            val viewModel = createViewModel()
            viewModel.onCampaignNameChange("Tomb of Annihilation")

            viewModel.addCampaign()
            advanceUntilIdle()

            assertEquals("Tomb of Annihilation", viewModel.uiState.value.campaignName)
        }

    // --------------------------------------------------------- delete campaign

    @Test
    fun requestDeleteCampaign_storesPendingTargetWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = campaign(id = 11, name = "Rime of the Frostmaiden")
            val viewModel = createViewModel()

            viewModel.requestDeleteCampaign(target)
            advanceUntilIdle()

            assertEquals(
                PendingCampaignDeletion(
                    campaignId = 11,
                    campaignName = "Rime of the Frostmaiden"
                ),
                viewModel.uiState.value.pendingDeletion
            )
            assertTrue(repository.deleteCampaignCalls.isEmpty())
        }

    @Test
    fun confirmDeleteCampaign_deletesAndClearsPendingTarget() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 11))

            viewModel.confirmDeleteCampaign()
            advanceUntilIdle()

            assertEquals(listOf(11), repository.deleteCampaignCalls)
            assertNull(viewModel.uiState.value.pendingDeletion)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun cancelDeleteCampaign_clearsPendingTargetWithoutDeleting() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 11))

            viewModel.cancelDeleteCampaign()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingDeletion)
            assertTrue(repository.deleteCampaignCalls.isEmpty())
        }

    @Test
    fun confirmDeleteCampaign_withoutPendingTarget_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.confirmDeleteCampaign()
            advanceUntilIdle()

            assertTrue(repository.deleteCampaignCalls.isEmpty())
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun confirmDeleteCampaign_afterCancel_doesNotDelete() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 11))
            viewModel.cancelDeleteCampaign()

            viewModel.confirmDeleteCampaign()
            advanceUntilIdle()

            assertTrue(repository.deleteCampaignCalls.isEmpty())
        }

    @Test
    fun confirmDeleteCampaign_whenRepositoryThrows_setsErrorAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.deleteCampaignError = IllegalStateException("delete failed")

            val viewModel = createViewModel()
            viewModel.requestDeleteCampaign(campaign(id = 11))

            viewModel.confirmDeleteCampaign()
            advanceUntilIdle()

            assertEquals("delete failed", viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }
}
