package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
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
 * The form, in both of its modes.
 *
 * The two things worth asserting are the ones a user would never see going wrong until it is too
 * late: that a new quest can be written from a title alone, and that editing one writes back every
 * field it was given - a form that quietly dropped the priority on a title change would look
 * perfectly fine on screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeQuestRepository

    @Before
    fun setUp() {
        repository = FakeQuestRepository()
    }

    /** The `quest/new/{campaignId}` route: a campaign and no quest. */
    private fun TestScope.createForNewQuest(campaignId: Int = CAMPAIGN_ID): QuestEditViewModel =
        QuestEditViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(QuestEditViewModel.CAMPAIGN_ID_KEY to campaignId)
            )
        ).also { advanceUntilIdle() }

    /** The `quest/{questId}/edit` route: a quest and no campaign. */
    private fun TestScope.createForExistingQuest(questId: Int = QUEST_ID): QuestEditViewModel =
        QuestEditViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(QuestEditViewModel.QUEST_ID_KEY to questId)
            )
        ).also { advanceUntilIdle() }

    // ------------------------------------------------- navigation argument

    @Test(expected = IllegalStateException::class)
    fun init_withNeitherArgument_fails() {
        // Exactly one of the two is always present; neither is a programming error.
        QuestEditViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle()
        )
    }

    // ----------------------------------------------------------- new quest

    @Test
    fun newQuest_startsEmptyWithTheOptionalFieldsFolded() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            val state = viewModel.uiState.value
            assertFalse(state.isEditing)
            assertEquals("", state.title)
            assertEquals("", state.description)
            // The promise of the module: a quest is a title and a line of description.
            assertFalse(state.areOptionalFieldsVisible)
            assertFalse(state.isLoading)
            // Nothing is read for a new quest.
            assertTrue(repository.getQuestByIdCalls.isEmpty())
        }

    @Test
    fun newQuest_startsWithTheDefaultsTheRepositoryWouldHaveUsed() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            val state = viewModel.uiState.value
            assertEquals(QuestCategory.SIDE, state.category)
            assertEquals(QuestStatus.NOT_STARTED, state.status)
            assertEquals(QuestPriority.NORMAL, state.priority)
            assertFalse(state.isHidden)
            assertEquals("", state.inGameDate)
        }

    @Test
    fun newQuest_withNothingButATitle_isSaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            viewModel.onTitleChange("The miller's missing daughter")
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeQuestRepository.AddQuestCall(
                        campaignId = CAMPAIGN_ID,
                        title = "The miller's missing daughter",
                        description = "",
                        category = QuestCategory.SIDE,
                        status = QuestStatus.NOT_STARTED,
                        priority = QuestPriority.NORMAL,
                        isHidden = false,
                        // An empty field is not a date: the column is nullable so that "no
                        // in-world date" is stored as nothing.
                        inGameDate = null
                    )
                ),
                repository.addQuestCalls
            )
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun newQuest_writesEveryFieldTheFormWasGiven() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            viewModel.onTitleChange("Strahd's invitation")
            viewModel.onDescriptionChange("Dinner at Ravenloft")
            viewModel.onCategorySelected(QuestCategory.MAIN)
            viewModel.onStatusSelected(QuestStatus.ACTIVE)
            viewModel.onPrioritySelected(QuestPriority.HIGH)
            viewModel.onHiddenChange(true)
            viewModel.onInGameDateChange("14th of Neth")
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    FakeQuestRepository.AddQuestCall(
                        campaignId = CAMPAIGN_ID,
                        title = "Strahd's invitation",
                        description = "Dinner at Ravenloft",
                        category = QuestCategory.MAIN,
                        status = QuestStatus.ACTIVE,
                        priority = QuestPriority.HIGH,
                        isHidden = true,
                        inGameDate = "14th of Neth"
                    )
                ),
                repository.addQuestCalls
            )
        }

    @Test
    fun save_trimsTheTextFields() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            viewModel.onTitleChange("  Escort Ireena  ")
            viewModel.onDescriptionChange("  past the Tser Pool  ")
            viewModel.onInGameDateChange("   ")
            viewModel.onSave()
            advanceUntilIdle()

            val call = repository.addQuestCalls.single()
            assertEquals("Escort Ireena", call.title)
            assertEquals("past the Tser Pool", call.description)
            // Whitespace is not an in-world date either.
            assertNull(call.inGameDate)
        }

    @Test
    fun save_withABlankTitle_isRefusedWithAReasonAndWritesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            viewModel.onTitleChange("   ")
            viewModel.onSave()
            advanceUntilIdle()

            // Says why instead of silently doing nothing, like adding a campaign does.
            assertEquals(
                UiText.of(R.string.quest_title_required),
                viewModel.uiState.value.titleError
            )
            assertTrue(repository.addQuestCalls.isEmpty())
            assertFalse(viewModel.uiState.value.isSaved)
        }

    @Test
    fun typingATitle_clearsTheRejectionItCaused() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()
            viewModel.onSave()
            advanceUntilIdle()

            viewModel.onTitleChange("A")

            assertNull(viewModel.uiState.value.titleError)
        }

    @Test
    fun save_whenTheWriteFails_reportsItAndDoesNotClaimToBeSaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.addQuestError = IllegalStateException("FOREIGN KEY constraint failed")
            val viewModel = createForNewQuest()

            viewModel.onTitleChange("Escort Ireena")
            viewModel.onSave()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(UiText.Raw("FOREIGN KEY constraint failed"), state.error)
            assertFalse(state.isSaved)
            assertFalse(state.isSaving)
        }

    // ------------------------------------------------------ existing quest

    @Test
    fun existingQuest_isReadIntoTheFormWithTheOptionalFieldsUnfolded() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(
                id = QUEST_ID,
                title = "Escort Ireena",
                description = "past the Tser Pool",
                category = QuestCategory.MAIN,
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.HIGH,
                isHidden = true,
                inGameDate = "14th of Neth"
            )

            val viewModel = createForExistingQuest()

            val state = viewModel.uiState.value
            assertTrue(state.isEditing)
            assertEquals("Escort Ireena", state.title)
            assertEquals("past the Tser Pool", state.description)
            assertEquals(QuestCategory.MAIN, state.category)
            assertEquals(QuestStatus.ACTIVE, state.status)
            assertEquals(QuestPriority.HIGH, state.priority)
            assertTrue(state.isHidden)
            assertEquals("14th of Neth", state.inGameDate)
            // Folding away fields the quest already has values in would hide what the user came
            // to change.
            assertTrue(state.areOptionalFieldsVisible)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun existingQuest_withoutAnInGameDate_showsAnEmptyField() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, inGameDate = null)

            val viewModel = createForExistingQuest()

            assertEquals("", viewModel.uiState.value.inGameDate)
        }

    @Test
    fun editingOnlyTheTitle_writesBackEveryOtherFieldUnchanged() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(
                id = QUEST_ID,
                title = "Escort Ireena",
                description = "past the Tser Pool",
                category = QuestCategory.MAIN,
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.HIGH,
                isHidden = true,
                inGameDate = "14th of Neth"
            )
            val viewModel = createForExistingQuest()

            viewModel.onTitleChange("Escort Ireena to Vallaki")
            viewModel.onSave()
            advanceUntilIdle()

            // The write replaces the fields as a whole, so anything the form failed to hand back
            // would be silently lost.
            assertEquals(
                listOf(
                    FakeQuestRepository.UpdateQuestCall(
                        id = QUEST_ID,
                        title = "Escort Ireena to Vallaki",
                        description = "past the Tser Pool",
                        category = QuestCategory.MAIN,
                        status = QuestStatus.ACTIVE,
                        priority = QuestPriority.HIGH,
                        isHidden = true,
                        inGameDate = "14th of Neth"
                    )
                ),
                repository.updateQuestCalls
            )
            assertTrue(viewModel.uiState.value.isSaved)
        }

    @Test
    fun editing_neverAddsASecondQuest() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID)
            val viewModel = createForExistingQuest()

            viewModel.onTitleChange("Renamed")
            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(repository.addQuestCalls.isEmpty())
        }

    @Test
    fun editing_readsTheQuestOnceSoALaterEmissionCannotOverwriteWhatIsTyped() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.questById.value = quest(id = QUEST_ID, title = "Escort Ireena")
            val viewModel = createForExistingQuest()

            viewModel.onTitleChange("Half typed name")
            // Something else changed the quest - a status change from the quest screen, say.
            repository.questById.value = quest(id = QUEST_ID, title = "Escort Ireena")
            advanceUntilIdle()

            assertEquals("Half typed name", viewModel.uiState.value.title)
        }

    @Test
    fun editing_aQuestThatIsAlreadyGone_saysSoAndRefusesToWrite() =
        runTest(mainDispatcherRule.testDispatcher) {

            // Deleted between the list and the form.
            repository.questById.value = null

            val viewModel = createForExistingQuest()

            val state = viewModel.uiState.value
            assertEquals(UiText.of(R.string.quest_not_found), state.error)
            assertFalse(state.isLoading)
            assertTrue(state.isMissing)
            // The save button is off: the reason is already on screen and a press could achieve
            // nothing.
            assertFalse(state.isSaveEnabled)

            viewModel.onTitleChange("Escort Ireena")
            viewModel.onSave()
            advanceUntilIdle()

            // Neither an update nor - which would be worse - a brand new quest.
            assertTrue(repository.updateQuestCalls.isEmpty())
            assertTrue(repository.addQuestCalls.isEmpty())
        }

    @Test
    fun editing_whenTheReadFails_reportsIt() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.getQuestByIdError = IllegalStateException("database is locked")

            val viewModel = createForExistingQuest()

            assertEquals(
                UiText.Raw("database is locked"),
                viewModel.uiState.value.error
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // -------------------------------------------------------- optional half

    @Test
    fun onToggleOptionalFields_foldsAndUnfolds() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()

            viewModel.onToggleOptionalFields()
            assertTrue(viewModel.uiState.value.areOptionalFieldsVisible)

            viewModel.onToggleOptionalFields()
            assertFalse(viewModel.uiState.value.areOptionalFieldsVisible)
        }

    @Test
    fun isSaveEnabled_isOffWhileAWriteIsInFlight() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()
            // An empty title does not disable the button - pressing it produces the reason.
            assertTrue(viewModel.uiState.value.isSaveEnabled)

            viewModel.onTitleChange("A title")
            viewModel.onSave()

            // Before the dispatcher is advanced the write is still in flight.
            assertFalse(viewModel.uiState.value.isSaveEnabled)

            advanceUntilIdle()
            assertEquals(1, repository.addQuestCalls.size)
        }

    @Test
    fun onSave_pressedTwiceWhileTheFirstWriteIsInFlight_writesOnce() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createForNewQuest()
            viewModel.onTitleChange("A title")

            viewModel.onSave()
            viewModel.onSave()
            advanceUntilIdle()

            // The button is disabled too, but the guard is what makes it true: a queued second
            // press must not add the same quest twice.
            assertEquals(1, repository.addQuestCalls.size)
        }

    private companion object {
        const val CAMPAIGN_ID = 1
        const val QUEST_ID = 5
    }
}
