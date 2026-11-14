package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.testing.FakeQuestConsequenceRepository
import com.example.digitaldungeon.testing.FakeQuestNoteRepository
import com.example.digitaldungeon.testing.FakeQuestObjectiveRepository
import com.example.digitaldungeon.testing.FakeQuestHistoryRepository
import com.example.digitaldungeon.testing.FakeQuestRelationRepository
import com.example.digitaldungeon.testing.FakeQuestSessionLinkRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.FakeQuestRewardRepository
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
 * One quest's screen: loading it, the quest being gone, and moving it between statuses.
 *
 * The status transitions are the interesting part. Nothing is terminal - a completed or failed
 * quest can be reopened - and that rule is asserted here rather than left to the UI, because the
 * repository does not police it either.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeQuestRepository
    private lateinit var objectiveRepository: FakeQuestObjectiveRepository
    private lateinit var rewardRepository: FakeQuestRewardRepository
    private lateinit var consequenceRepository: FakeQuestConsequenceRepository
    private lateinit var noteRepository: FakeQuestNoteRepository
    private lateinit var relationRepository: FakeQuestRelationRepository
    private lateinit var historyRepository: FakeQuestHistoryRepository
    private lateinit var sessionLinkRepository: FakeQuestSessionLinkRepository
    private lateinit var sessionRepository: FakeSessionRepository

    @Before
    fun setUp() {
        repository = FakeQuestRepository()
        objectiveRepository = FakeQuestObjectiveRepository()
        rewardRepository = FakeQuestRewardRepository()
        consequenceRepository = FakeQuestConsequenceRepository()
        noteRepository = FakeQuestNoteRepository()
        relationRepository = FakeQuestRelationRepository()
        historyRepository = FakeQuestHistoryRepository()
        sessionLinkRepository = FakeQuestSessionLinkRepository()
        sessionRepository = FakeSessionRepository()
    }

    private fun TestScope.createViewModel(questId: Int = QUEST_ID): QuestDetailViewModel =
        QuestDetailViewModel(
            repository = repository,
            objectiveRepository = objectiveRepository,
            rewardRepository = rewardRepository,
            consequenceRepository = consequenceRepository,
            noteRepository = noteRepository,
            relationRepository = relationRepository,
            historyRepository = historyRepository,
            sessionLinkRepository = sessionLinkRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle(
                mapOf(QuestDetailViewModel.QUEST_ID_KEY to questId)
            )
        ).also { advanceUntilIdle() }

    // ------------------------------------------------- navigation argument

    @Test
    fun init_readsQuestIdFromSavedStateHandle() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(questId = 13)

            assertEquals(13, viewModel.uiState.value.questId)
            assertEquals(listOf(13), repository.getQuestByIdCalls)
        }

    @Test(expected = IllegalStateException::class)
    fun init_withoutQuestIdArgument_fails() {
        QuestDetailViewModel(
            repository = repository,
            objectiveRepository = objectiveRepository,
            rewardRepository = rewardRepository,
            consequenceRepository = consequenceRepository,
            noteRepository = noteRepository,
            relationRepository = relationRepository,
            historyRepository = historyRepository,
            sessionLinkRepository = sessionLinkRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun init_asksForTheObjectivesOfTheSameQuest() =
        runTest(mainDispatcherRule.testDispatcher) {

            createViewModel(questId = 13)

            // The objectives hang off the quest, so the two subscriptions must name the same id.
            assertEquals(listOf(13), objectiveRepository.getObjectivesForQuestCalls)
        }

    @Test
    fun anObjectiveFailure_leavesTheQuestErrorChannelAlone() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID)
            objectiveRepository.getObjectivesForQuestError = IllegalStateException("no such table")

            val viewModel = createViewModel()

            // Two channels, so one failure cannot pass itself off as the other's.
            assertNull(viewModel.uiState.value.error)
            assertEquals(
                UiText.Raw("no such table"),
                viewModel.objectivesUiState.value.error
            )
        }

    // ------------------------------------------------------------- loading

    @Test
    fun init_loadsTheQuestAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = quest(id = QUEST_ID, title = "Escort Ireena")
            repository.questById.value = stored

            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertEquals(stored, state.quest)
            assertFalse(state.isLoading)
            assertFalse(state.isMissing)
            assertNull(state.error)
        }

    @Test
    fun init_whenThereIsNoSuchQuest_saysItIsMissingWithoutCallingItAnError() =
        runTest(mainDispatcherRule.testDispatcher) {

            // The flow emits null, which is what a deleted quest looks like.
            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertNull(state.quest)
            assertFalse(state.isLoading)
            assertTrue(state.isMissing)
            // A quest that is gone is a fact about the journal, not a failure of the screen.
            assertNull(state.error)
        }

    @Test
    fun init_whenTheQuestFlowFails_reportsItAndStopsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.getQuestByIdError = IllegalStateException("database is locked")

            val viewModel = createViewModel()

            assertEquals(
                UiText.Raw("database is locked"),
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun loadedQuest_isObservedSoALaterChangeArrivesOnItsOwn() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.NOT_STARTED)
            val viewModel = createViewModel()

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.ACTIVE)
            advanceUntilIdle()

            // No second copy of the quest to keep in sync: the screen is drawn from the flow.
            assertEquals(QuestStatus.ACTIVE, viewModel.uiState.value.quest?.status)
        }

    // ------------------------------------------------------ status changes

    @Test
    fun onStatusSelected_writesTheNewStatusThrough() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.NOT_STARTED)
            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()

            assertEquals(
                listOf(FakeQuestRepository.UpdateStatusCall(QUEST_ID, QuestStatus.ACTIVE)),
                repository.updateStatusCalls
            )
        }

    @Test
    fun onStatusSelected_reopensACompletedQuest() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.COMPLETED)
            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()

            // Nothing is terminal: a quest everyone thought was over is a normal way for a
            // campaign to continue.
            assertEquals(
                listOf(FakeQuestRepository.UpdateStatusCall(QUEST_ID, QuestStatus.ACTIVE)),
                repository.updateStatusCalls
            )
        }

    @Test
    fun onStatusSelected_reopensAFailedQuest() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.FAILED)
            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.PAUSED)
            advanceUntilIdle()

            assertEquals(
                listOf(FakeQuestRepository.UpdateStatusCall(QUEST_ID, QuestStatus.PAUSED)),
                repository.updateStatusCalls
            )
        }

    @Test
    fun onStatusSelected_withTheStatusItAlreadyHas_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.ACTIVE)
            val viewModel = createViewModel()

            // Which is what tapping the already selected chip does.
            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()

            assertTrue(repository.updateStatusCalls.isEmpty())
        }

    @Test
    fun onStatusSelected_withoutALoadedQuest_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()

            assertTrue(repository.updateStatusCalls.isEmpty())
        }

    @Test
    fun onStatusSelected_whenTheWriteFails_reportsIt() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.NOT_STARTED)
            repository.updateQuestStatusError = IllegalStateException("disk I/O error")
            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()

            assertEquals(
                UiText.Raw("disk I/O error"),
                viewModel.uiState.value.error
            )
        }

    @Test
    fun onStatusSelected_thatSucceeds_takesAStaleFailureOffTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, status = QuestStatus.NOT_STARTED)
            repository.updateQuestStatusError = IllegalStateException("disk I/O error")
            val viewModel = createViewModel()

            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            advanceUntilIdle()
            assertEquals(UiText.Raw("disk I/O error"), viewModel.uiState.value.error)

            repository.updateQuestStatusError = null
            viewModel.onStatusSelected(QuestStatus.PAUSED)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.error)
        }

    private companion object {
        const val QUEST_ID = 5
    }
}
