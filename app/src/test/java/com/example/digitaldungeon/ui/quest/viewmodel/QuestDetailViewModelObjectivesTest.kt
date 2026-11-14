package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.repository.QuestObjectiveRepository
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
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
import com.example.digitaldungeon.testing.questObjective
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
 * The objectives block of the quest screen: the form, the statuses, the order and deletion.
 *
 * The two things worth asserting hardest are the ones a user would not notice going wrong until
 * their notes were already wrong: that editing the text of a step does not quietly drop its
 * "optional" flag, and that a reorder at the edge of the list writes nothing at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestDetailViewModelObjectivesTest {

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
        repository.questById.value = quest(id = QUEST_ID)
    }

    private fun TestScope.createViewModel(): QuestDetailViewModel =
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
                mapOf(QuestDetailViewModel.QUEST_ID_KEY to QUEST_ID)
            )
        ).also { advanceUntilIdle() }

    /** Three steps in a fixed order, so "move" and "edge" have something to mean. */
    private fun seedObjectives() {
        objectiveRepository.objectives.value = listOf(
            questObjective(id = 1, questId = QUEST_ID, title = "First", orderIndex = 0),
            questObjective(id = 2, questId = QUEST_ID, title = "Second", orderIndex = 1),
            questObjective(id = 3, questId = QUEST_ID, title = "Third", orderIndex = 2)
        )
    }

    // ------------------------------------------------------------- loading

    @Test
    fun init_loadsTheObjectivesInOrderAndClearsLoading() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()

            val viewModel = createViewModel()

            val state = viewModel.objectivesUiState.value
            assertEquals(listOf("First", "Second", "Third"), state.objectives.map { it.title })
            assertFalse(state.isLoading)
            assertFalse(state.hasNoObjectives)
            assertNull(state.error)
        }

    @Test
    fun aQuestWithoutObjectives_stopsLoadingAndSaysSo() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            val state = viewModel.objectivesUiState.value
            // A quest with no steps is a complete quest, not one still loading.
            assertFalse(state.isLoading)
            assertTrue(state.hasNoObjectives)
        }

    @Test
    fun aLaterEmission_takesAStaleFailureOffTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            objectiveRepository.deleteObjectiveError = IllegalStateException("disk I/O error")
            val viewModel = createViewModel()

            viewModel.onObjectiveDeleteRequest(questObjective(id = 1))
            viewModel.onConfirmObjectiveDelete()
            advanceUntilIdle()
            assertEquals(
                UiText.Raw("disk I/O error"),
                viewModel.objectivesUiState.value.error
            )

            objectiveRepository.objectives.value = emptyList()
            advanceUntilIdle()

            assertNull(viewModel.objectivesUiState.value.error)
        }

    // ---------------------------------------------------------------- add

    @Test
    fun addObjective_withNothingButATitle_appendsItWithTheFormDefaults() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("Reach the Tser Pool")
            viewModel.onAddObjective()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeQuestObjectiveRepository.AddObjectiveCall(
                        questId = QUEST_ID,
                        title = "Reach the Tser Pool",
                        description = "",
                        isOptional = false
                    )
                ),
                objectiveRepository.addObjectiveCalls
            )
        }

    @Test
    fun addObjective_writesTheDetailsAndTheOptionalFlag() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("Hear out the Vistani")
            viewModel.onObjectiveDescriptionChange("Madam Eva reads the cards, if asked.")
            viewModel.onObjectiveOptionalChange(true)
            viewModel.onAddObjective()
            advanceUntilIdle()

            assertEquals(
                FakeQuestObjectiveRepository.AddObjectiveCall(
                    questId = QUEST_ID,
                    title = "Hear out the Vistani",
                    description = "Madam Eva reads the cards, if asked.",
                    isOptional = true
                ),
                objectiveRepository.addObjectiveCalls.single()
            )
        }

    @Test
    fun addObjective_trimsTheText() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("  Reach the pool  ")
            viewModel.onObjectiveDescriptionChange("  before dusk  ")
            viewModel.onAddObjective()
            advanceUntilIdle()

            val call = objectiveRepository.addObjectiveCalls.single()
            assertEquals("Reach the pool", call.title)
            assertEquals("before dusk", call.description)
        }

    @Test
    fun addObjective_clearsTheFormSoTheNextStepStartsEmpty() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("Reach the pool")
            viewModel.onObjectiveDescriptionChange("before dusk")
            viewModel.onObjectiveOptionalChange(true)
            viewModel.onAddObjective()
            advanceUntilIdle()

            val state = viewModel.objectivesUiState.value
            assertEquals("", state.formTitle)
            assertEquals("", state.formDescription)
            // Carrying the flag over would quietly mark the next step optional too.
            assertFalse(state.formIsOptional)
            assertFalse(state.isEditing)
        }

    @Test
    fun addObjective_withABlankTitle_isRefusedWithAReasonAndWritesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("   ")
            viewModel.onAddObjective()
            advanceUntilIdle()

            assertEquals(
                UiText.of(R.string.quest_objective_title_required),
                viewModel.objectivesUiState.value.formError
            )
            assertTrue(objectiveRepository.addObjectiveCalls.isEmpty())
        }

    @Test
    fun typingAnObjectiveTitle_clearsTheRejectionItCaused() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.onAddObjective()
            advanceUntilIdle()

            viewModel.onObjectiveTitleChange("A")

            assertNull(viewModel.objectivesUiState.value.formError)
        }

    @Test
    fun addObjective_whenTheWriteFails_reportsItAndKeepsWhatWasTyped() =
        runTest(mainDispatcherRule.testDispatcher) {

            objectiveRepository.addObjectiveError = IllegalStateException("FOREIGN KEY failed")
            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("Reach the pool")
            viewModel.onAddObjective()
            advanceUntilIdle()

            val state = viewModel.objectivesUiState.value
            assertEquals(UiText.Raw("FOREIGN KEY failed"), state.error)
            // The form is not cleared on a failure: retyping what was just refused is work the
            // user should not have to repeat.
            assertEquals("Reach the pool", state.formTitle)
        }

    // --------------------------------------------------------------- edit

    @Test
    fun editRequest_loadsTheObjectiveIntoTheForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = questObjective(
                id = 3,
                questId = QUEST_ID,
                title = "Hear out the Vistani",
                description = "Madam Eva reads the cards.",
                isOptional = true
            )
            objectiveRepository.objectives.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onEditObjectiveRequest(target)

            val state = viewModel.objectivesUiState.value
            assertTrue(state.isEditing)
            assertEquals(3, state.editingObjectiveId)
            assertEquals("Hear out the Vistani", state.formTitle)
            assertEquals("Madam Eva reads the cards.", state.formDescription)
            assertTrue(state.formIsOptional)
        }

    @Test
    fun editingOnlyTheTitle_writesBackTheOptionalFlagUnchanged() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = questObjective(
                id = 3,
                questId = QUEST_ID,
                title = "Hear out the Vistani",
                description = "Madam Eva reads the cards.",
                isOptional = true
            )
            objectiveRepository.objectives.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onEditObjectiveRequest(target)
            viewModel.onObjectiveTitleChange("Hear out Madam Eva")
            viewModel.onUpdateObjective()
            advanceUntilIdle()

            // The write replaces the three fields as a whole, so anything the form failed to hand
            // back would be silently lost.
            assertEquals(
                listOf(
                    FakeQuestObjectiveRepository.UpdateObjectiveCall(
                        id = 3,
                        title = "Hear out Madam Eva",
                        description = "Madam Eva reads the cards.",
                        isOptional = true
                    )
                ),
                objectiveRepository.updateObjectiveCalls
            )
        }

    @Test
    fun updateObjective_clearsTheFormBackToAdding() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = questObjective(id = 1, questId = QUEST_ID)
            objectiveRepository.objectives.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onEditObjectiveRequest(target)
            viewModel.onUpdateObjective()
            advanceUntilIdle()

            val state = viewModel.objectivesUiState.value
            assertFalse(state.isEditing)
            assertEquals("", state.formTitle)
        }

    @Test
    fun updateObjective_withABlankTitle_isRefusedAndKeepsTheFormOpen() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = questObjective(id = 1, questId = QUEST_ID)
            objectiveRepository.objectives.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onEditObjectiveRequest(target)
            viewModel.onObjectiveTitleChange("  ")
            viewModel.onUpdateObjective()
            advanceUntilIdle()

            val state = viewModel.objectivesUiState.value
            assertEquals(
                UiText.of(R.string.quest_objective_title_required),
                state.formError
            )
            assertTrue(objectiveRepository.updateObjectiveCalls.isEmpty())
            // Still editing: closing the form would throw away the text it refused to save.
            assertTrue(state.isEditing)
        }

    @Test
    fun updateObjective_withoutAnObjectiveBeingEdited_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onObjectiveTitleChange("Something")
            viewModel.onUpdateObjective()
            advanceUntilIdle()

            assertTrue(objectiveRepository.updateObjectiveCalls.isEmpty())
            // And in particular not added as a new one.
            assertTrue(objectiveRepository.addObjectiveCalls.isEmpty())
        }

    @Test
    fun cancelEdit_emptiesTheFormAndGoesBackToAdding() =
        runTest(mainDispatcherRule.testDispatcher) {

            val target = questObjective(id = 1, questId = QUEST_ID, isOptional = true)
            objectiveRepository.objectives.value = listOf(target)
            val viewModel = createViewModel()

            viewModel.onEditObjectiveRequest(target)
            viewModel.onCancelObjectiveEdit()

            val state = viewModel.objectivesUiState.value
            assertFalse(state.isEditing)
            assertEquals("", state.formTitle)
            assertFalse(state.formIsOptional)
            assertTrue(objectiveRepository.updateObjectiveCalls.isEmpty())
        }

    @Test
    fun editRequest_takesAPendingDeletionOffTheScreen() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val first = viewModel.objectivesUiState.value.objectives.first()

            viewModel.onObjectiveDeleteRequest(first)
            viewModel.onEditObjectiveRequest(first)

            // One question at a time.
            assertNull(viewModel.objectivesUiState.value.pendingDeletion)
        }

    // ------------------------------------------------------------- status

    @Test
    fun onObjectiveStatusSelected_writesTheNewStatusThrough() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val first = viewModel.objectivesUiState.value.objectives.first()

            viewModel.onObjectiveStatusSelected(first, QuestObjectiveStatus.COMPLETED)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeQuestObjectiveRepository.UpdateStatusCall(
                        1,
                        QuestObjectiveStatus.COMPLETED
                    )
                ),
                objectiveRepository.updateStatusCalls
            )
        }

    @Test
    fun onObjectiveStatusSelected_untiksAStepMarkedDoneByMistake() =
        runTest(mainDispatcherRule.testDispatcher) {

            objectiveRepository.objectives.value = listOf(
                questObjective(id = 1, questId = QUEST_ID, status = QuestObjectiveStatus.COMPLETED)
            )
            val viewModel = createViewModel()
            val done = viewModel.objectivesUiState.value.objectives.single()

            viewModel.onObjectiveStatusSelected(done, QuestObjectiveStatus.NOT_STARTED)
            advanceUntilIdle()

            assertEquals(
                FakeQuestObjectiveRepository.UpdateStatusCall(
                    1,
                    QuestObjectiveStatus.NOT_STARTED
                ),
                objectiveRepository.updateStatusCalls.single()
            )
        }

    @Test
    fun onObjectiveStatusSelected_withTheStatusItAlreadyHas_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val first = viewModel.objectivesUiState.value.objectives.first()

            viewModel.onObjectiveStatusSelected(first, first.status)
            advanceUntilIdle()

            assertTrue(objectiveRepository.updateStatusCalls.isEmpty())
        }

    @Test
    fun onObjectiveStatusSelected_whenTheWriteFails_reportsIt() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            objectiveRepository.updateObjectiveStatusError = IllegalStateException("locked")
            val viewModel = createViewModel()
            val first = viewModel.objectivesUiState.value.objectives.first()

            viewModel.onObjectiveStatusSelected(first, QuestObjectiveStatus.COMPLETED)
            advanceUntilIdle()

            assertEquals(UiText.Raw("locked"), viewModel.objectivesUiState.value.error)
        }

    // -------------------------------------------------------------- order

    @Test
    fun canMoveUpAndDown_areFalseOnlyAtTheEnds() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val state = viewModel.objectivesUiState.value

            assertFalse(state.canMoveUp(0))
            assertTrue(state.canMoveDown(0))

            assertTrue(state.canMoveUp(1))
            assertTrue(state.canMoveDown(1))

            assertTrue(state.canMoveUp(2))
            assertFalse(state.canMoveDown(2))
        }

    @Test
    fun theOnlyObjective_cannotMoveEitherWay() =
        runTest(mainDispatcherRule.testDispatcher) {

            objectiveRepository.objectives.value = listOf(
                questObjective(id = 1, questId = QUEST_ID)
            )
            val viewModel = createViewModel()
            val state = viewModel.objectivesUiState.value

            assertFalse(state.canMoveUp(0))
            assertFalse(state.canMoveDown(0))
        }

    @Test
    fun moveUp_shiftsTheObjectiveOnePosition() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onMoveObjectiveUp(second)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeQuestObjectiveRepository.MoveObjectiveCall(
                        2,
                        QuestObjectiveRepository.MoveDirection.UP
                    )
                ),
                objectiveRepository.moveObjectiveCalls
            )
        }

    @Test
    fun moveDown_shiftsTheObjectiveOnePosition() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onMoveObjectiveDown(second)
            advanceUntilIdle()

            assertEquals(
                FakeQuestObjectiveRepository.MoveObjectiveCall(
                    2,
                    QuestObjectiveRepository.MoveDirection.DOWN
                ),
                objectiveRepository.moveObjectiveCalls.single()
            )
        }

    @Test
    fun movingTheFirstObjectiveUp_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val first = viewModel.objectivesUiState.value.objectives.first()

            viewModel.onMoveObjectiveUp(first)
            advanceUntilIdle()

            // The arrow is disabled too, but the guard is what makes it true.
            assertTrue(objectiveRepository.moveObjectiveCalls.isEmpty())
        }

    @Test
    fun movingTheLastObjectiveDown_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val last = viewModel.objectivesUiState.value.objectives.last()

            viewModel.onMoveObjectiveDown(last)
            advanceUntilIdle()

            assertTrue(objectiveRepository.moveObjectiveCalls.isEmpty())
        }

    @Test
    fun movingAnObjectiveThatIsNoLongerInTheList_writesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()

            viewModel.onMoveObjectiveUp(questObjective(id = 99, questId = QUEST_ID))
            advanceUntilIdle()

            assertTrue(objectiveRepository.moveObjectiveCalls.isEmpty())
        }

    @Test
    fun move_whenTheWriteFails_reportsIt() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            objectiveRepository.moveObjectiveError = IllegalStateException("locked")
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onMoveObjectiveUp(second)
            advanceUntilIdle()

            assertEquals(UiText.Raw("locked"), viewModel.objectivesUiState.value.error)
        }

    // ------------------------------------------------------------- delete

    @Test
    fun deleteRequest_armsTheDialogWithoutDeletingAnything() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onObjectiveDeleteRequest(second)
            advanceUntilIdle()

            val pending = viewModel.objectivesUiState.value.pendingDeletion
            assertEquals(2, pending?.objectiveId)
            assertEquals("Second", pending?.objectiveTitle)
            assertTrue(objectiveRepository.deleteObjectiveCalls.isEmpty())
        }

    @Test
    fun confirmDelete_deletesTheArmedObjectiveAndClosesTheDialog() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onObjectiveDeleteRequest(second)
            viewModel.onConfirmObjectiveDelete()
            advanceUntilIdle()

            assertEquals(listOf(2), objectiveRepository.deleteObjectiveCalls)
            assertNull(viewModel.objectivesUiState.value.pendingDeletion)
        }

    @Test
    fun cancelDelete_closesTheDialogAndDeletesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onObjectiveDeleteRequest(second)
            viewModel.onCancelObjectiveDelete()
            advanceUntilIdle()

            assertNull(viewModel.objectivesUiState.value.pendingDeletion)
            assertTrue(objectiveRepository.deleteObjectiveCalls.isEmpty())
        }

    @Test
    fun confirmDelete_withoutAnArmedObjective_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onConfirmObjectiveDelete()
            advanceUntilIdle()

            assertTrue(objectiveRepository.deleteObjectiveCalls.isEmpty())
        }

    @Test
    fun deletingTheObjectiveBeingEdited_closesTheForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val second = viewModel.objectivesUiState.value.objectives[1]

            viewModel.onEditObjectiveRequest(second)
            viewModel.onObjectiveDeleteRequest(second)
            viewModel.onConfirmObjectiveDelete()
            advanceUntilIdle()

            // Leaving the form open would offer to save a row that no longer exists.
            val state = viewModel.objectivesUiState.value
            assertFalse(state.isEditing)
            assertEquals("", state.formTitle)
        }

    @Test
    fun deletingAnotherObjective_leavesTheFormBeingEditedAlone() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedObjectives()
            val viewModel = createViewModel()
            val objectives = viewModel.objectivesUiState.value.objectives

            viewModel.onEditObjectiveRequest(objectives[0])
            viewModel.onObjectiveDeleteRequest(objectives[1])
            viewModel.onConfirmObjectiveDelete()
            advanceUntilIdle()

            val state = viewModel.objectivesUiState.value
            assertTrue(state.isEditing)
            assertEquals("First", state.formTitle)
        }

    private companion object {
        const val QUEST_ID = 5
    }
}
