package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.testing.FakeQuestRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.quest
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
 * Loading, the navigation argument and deletion.
 *
 * The isolation of two campaigns' quests is asserted here as well, at the only place a unit test
 * can see it: which campaign the repository was asked about. That the query itself is scoped is
 * `QuestDaoTest`'s business.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeQuestRepository

    @Before
    fun setUp() {
        repository = FakeQuestRepository()
    }

    /** Creates the ViewModel and lets the `init` subscription settle. */
    private fun TestScope.createViewModel(campaignId: Int = CAMPAIGN_ID): QuestListViewModel =
        QuestListViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(QuestListViewModel.CAMPAIGN_ID_KEY to campaignId)
            )
        ).also { advanceUntilIdle() }

    // ------------------------------------------------- navigation argument

    @Test
    fun init_readsCampaignIdFromSavedStateHandle_andOnlyAsksAboutThatCampaign() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(campaignId = 42)

            assertEquals(42, viewModel.uiState.value.campaignId)
            // The single thing that keeps two campaigns' quests apart on this screen.
            assertEquals(listOf(42), repository.getQuestsForCampaignCalls)
        }

    @Test(expected = IllegalStateException::class)
    fun init_withoutCampaignIdArgument_fails() {
        // A missing navigation argument is a programming error, not campaign number 0.
        QuestListViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle()
        )
    }

    // ------------------------------------------------------------- loading

    @Test
    fun init_loadsQuestsIntoStateAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(
                quest(id = 1, title = "Escort Ireena"),
                quest(id = 2, title = "Find the amber shard")
            )
            repository.quests.value = stored

            val viewModel = createViewModel()

            assertEquals(stored, viewModel.uiState.value.quests)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun init_withoutQuests_stopsLoadingAndSaysTheListIsEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            // An empty list is an answer: a campaign without quests must not spin forever.
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.hasNoQuests)
            assertFalse(viewModel.uiState.value.hasNoMatches)
        }

    @Test
    fun init_whenTheQuestFlowFails_reportsItAndStopsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.getQuestsForCampaignError = IllegalStateException("no such table")

            val viewModel = createViewModel()

            assertEquals(
                UiText.Raw("no such table"),
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun aLaterEmission_takesAStaleFailureOffTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.quests.value = listOf(quest(id = 1))
            repository.deleteQuestError = IllegalStateException("disk I/O error")
            val viewModel = createViewModel()

            viewModel.onDeleteRequest(quest(id = 1))
            viewModel.onConfirmDelete()
            advanceUntilIdle()
            assertEquals(UiText.Raw("disk I/O error"), viewModel.uiState.value.error)

            // The next write that does land makes the list emit again, and a message about
            // something that has since worked is worse than no message.
            repository.quests.value = emptyList()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
        }

    // -------------------------------------------------------------- delete

    @Test
    fun onDeleteRequest_armsTheDialogWithoutDeletingAnything() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = quest(id = 7, title = "Burgomaster's funeral")
            repository.quests.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onDeleteRequest(target)
            advanceUntilIdle()

            val pending = viewModel.uiState.value.pendingDeletion
            assertEquals(7, pending?.questId)
            // The title travels with the request: the dialog has to name what disappears.
            assertEquals("Burgomaster's funeral", pending?.questTitle)
            assertTrue(repository.deleteQuestCalls.isEmpty())
        }

    @Test
    fun onConfirmDelete_deletesTheArmedQuestAndClosesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = quest(id = 7)
            repository.quests.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onDeleteRequest(target)
            viewModel.onConfirmDelete()
            advanceUntilIdle()

            assertEquals(listOf(7), repository.deleteQuestCalls)
            assertNull(viewModel.uiState.value.pendingDeletion)
        }

    @Test
    fun onCancelDelete_closesTheDialogAndDeletesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = quest(id = 7)
            repository.quests.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onDeleteRequest(target)
            viewModel.onCancelDelete()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.pendingDeletion)
            assertTrue(repository.deleteQuestCalls.isEmpty())
        }

    @Test
    fun onConfirmDelete_withoutAnArmedQuest_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onConfirmDelete()
            advanceUntilIdle()

            assertTrue(repository.deleteQuestCalls.isEmpty())
        }

    @Test
    fun onConfirmDelete_whenTheWriteFails_reportsItAndKeepsTheDialogClosed() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = quest(id = 7)
            repository.quests.value = listOf(target)
            repository.deleteQuestError = IllegalStateException("FOREIGN KEY failed")
            val viewModel = createViewModel()

            viewModel.onDeleteRequest(target)
            viewModel.onConfirmDelete()
            advanceUntilIdle()

            assertEquals(
                UiText.Raw("FOREIGN KEY failed"),
                viewModel.uiState.value.error
            )
            assertNull(viewModel.uiState.value.pendingDeletion)
        }

    private companion object {
        const val CAMPAIGN_ID = 1
    }
}
