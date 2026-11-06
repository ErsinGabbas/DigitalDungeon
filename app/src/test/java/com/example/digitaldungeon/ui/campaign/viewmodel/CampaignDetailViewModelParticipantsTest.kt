package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.FakeSessionRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import com.example.digitaldungeon.testing.participant
import com.example.digitaldungeon.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The participant block of the campaign detail screen: form validation, index based
 * editing and the unsaved-changes bookkeeping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignDetailViewModelParticipantsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var campaignRepository: FakeCampaignRepository
    private lateinit var sessionRepository: FakeSessionRepository

    @Before
    fun setUp() {
        campaignRepository = FakeCampaignRepository()
        sessionRepository = FakeSessionRepository()
    }

    /**
     * Creates the ViewModel with [stored] already persisted for the campaign and lets
     * the `init` subscriptions settle, so the editable collection starts out clean.
     */
    private fun TestScope.createViewModel(
        stored: List<EventParticipant> = emptyList()
    ): CampaignDetailViewModel {

        campaignRepository.campaignById.value =
            campaign(id = CAMPAIGN_ID, participants = stored)

        return CampaignDetailViewModel(
            campaignRepository = campaignRepository,
            sessionRepository = sessionRepository,
            savedStateHandle = SavedStateHandle(
                mapOf(CampaignDetailViewModel.CAMPAIGN_ID_KEY to CAMPAIGN_ID)
            )
        ).also { advanceUntilIdle() }
    }

    private fun CampaignDetailViewModel.fillForm(
        name: String,
        level: String,
        maxHp: String
    ) {
        onParticipantNameChange(name)
        onParticipantLevelChange(level)
        onParticipantMaxHpChange(maxHp)
    }

    // ------------------------------------------------------- form validation

    @Test
    fun addParticipant_withValidInput_appendsParticipant() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")

            viewModel.addParticipant()

            assertEquals(
                listOf(EventParticipant(name = "Gandalf", level = 5, maxHp = 42)),
                viewModel.participantsUiState.value.participants
            )
            assertNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun addParticipant_withValidInput_clearsForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")

            viewModel.addParticipant()

            val state = viewModel.participantsUiState.value
            assertEquals("", state.participantName)
            assertEquals("", state.participantLevel)
            assertEquals("", state.participantMaxHp)
            assertNull(state.editingIndex)
        }

    @Test
    fun addParticipant_trimsSurroundingWhitespaceFromName() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "  Gandalf  ", level = " 5 ", maxHp = " 42 ")

            viewModel.addParticipant()

            assertEquals(
                EventParticipant(name = "Gandalf", level = 5, maxHp = 42),
                viewModel.participantsUiState.value.participants.single()
            )
        }

    @Test
    fun addParticipant_withBlankName_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "   ", level = "5", maxHp = "42")

            viewModel.addParticipant()

            assertEquals(
                UiText.of(R.string.campaign_participant_name_required),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withNonNumericLevel_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "five", maxHp = "42")

            viewModel.addParticipant()

            assertEquals(
                UiText.of(R.string.campaign_participant_level_not_a_number),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withLevelBelowMinimum_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(
                name = "Gandalf",
                level = (CampaignDetailViewModel.MIN_LEVEL - 1).toString(),
                maxHp = "42"
            )

            viewModel.addParticipant()

            assertEquals(
                UiText.of(
                    R.string.campaign_participant_level_out_of_range,
                    CampaignDetailViewModel.MIN_LEVEL,
                    CampaignDetailViewModel.MAX_LEVEL
                ),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    /**
     * The test above derives its input from MIN_LEVEL, so it follows the constant
     * wherever it moves and cannot catch a change of the policy itself. This one
     * pins the policy: a level of 0 is not a valid character level.
     */
    @Test
    fun addParticipant_withLevelZero_isRejected() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "0", maxHp = "42")

            viewModel.addParticipant()

            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
            assertNotNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun addParticipant_withMaxHpZero_isRejected() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "0")

            viewModel.addParticipant()

            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
            assertNotNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun addParticipant_withLevelAboveMaximum_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(
                name = "Gandalf",
                level = (CampaignDetailViewModel.MAX_LEVEL + 1).toString(),
                maxHp = "42"
            )

            viewModel.addParticipant()

            assertEquals(
                UiText.of(
                    R.string.campaign_participant_level_out_of_range,
                    CampaignDetailViewModel.MIN_LEVEL,
                    CampaignDetailViewModel.MAX_LEVEL
                ),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withNonNumericMaxHp_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "lots")

            viewModel.addParticipant()

            assertEquals(
                UiText.of(R.string.campaign_participant_max_hp_not_a_number),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withMaxHpBelowMinimum_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(
                name = "Gandalf",
                level = "5",
                maxHp = (CampaignDetailViewModel.MIN_MAX_HP - 1).toString()
            )

            viewModel.addParticipant()

            assertEquals(
                UiText.of(
                    R.string.campaign_participant_max_hp_out_of_range,
                    CampaignDetailViewModel.MIN_MAX_HP,
                    CampaignDetailViewModel.MAX_MAX_HP
                ),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withMaxHpAboveMaximum_setsErrorAndAddsNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(
                name = "Gandalf",
                level = "5",
                maxHp = (CampaignDetailViewModel.MAX_MAX_HP + 1).toString()
            )

            viewModel.addParticipant()

            assertEquals(
                UiText.of(
                    R.string.campaign_participant_max_hp_out_of_range,
                    CampaignDetailViewModel.MIN_MAX_HP,
                    CampaignDetailViewModel.MAX_MAX_HP
                ),
                viewModel.participantsUiState.value.error
            )
            assertTrue(viewModel.participantsUiState.value.participants.isEmpty())
        }

    @Test
    fun addParticipant_withBoundaryValues_isAccepted() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(
                name = "Edge",
                level = CampaignDetailViewModel.MAX_LEVEL.toString(),
                maxHp = CampaignDetailViewModel.MAX_MAX_HP.toString()
            )

            viewModel.addParticipant()

            assertEquals(
                EventParticipant(
                    name = "Edge",
                    level = CampaignDetailViewModel.MAX_LEVEL,
                    maxHp = CampaignDetailViewModel.MAX_MAX_HP
                ),
                viewModel.participantsUiState.value.participants.single()
            )
            assertNull(viewModel.participantsUiState.value.error)
        }

    @Test
    fun editingForm_clearsPreviousValidationError() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "", level = "5", maxHp = "42")
            viewModel.addParticipant()
            assertNotNull(viewModel.participantsUiState.value.error)

            viewModel.onParticipantNameChange("Gandalf")

            assertNull(viewModel.participantsUiState.value.error)
        }

    // -------------------------------------------------------- editing by index

    @Test
    fun editParticipant_fillsFormWithSelectedParticipant() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn", level = 4, maxHp = 30),
                    participant(name = "Legolas", level = 6, maxHp = 28)
                )
            )

            viewModel.editParticipant(1)

            val state = viewModel.participantsUiState.value
            assertEquals(1, state.editingIndex)
            assertEquals("Legolas", state.participantName)
            assertEquals("6", state.participantLevel)
            assertEquals("28", state.participantMaxHp)
        }

    @Test
    fun editParticipant_withOutOfBoundsIndex_leavesFormUntouched() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(stored = listOf(participant(name = "Aragorn")))

            viewModel.editParticipant(5)

            assertNull(viewModel.participantsUiState.value.editingIndex)
            assertEquals("", viewModel.participantsUiState.value.participantName)
        }

    @Test
    fun updateParticipant_replacesOnlyTheEditedIndex() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn", level = 4, maxHp = 30),
                    participant(name = "Legolas", level = 6, maxHp = 28),
                    participant(name = "Gimli", level = 5, maxHp = 35)
                )
            )

            viewModel.editParticipant(1)
            viewModel.fillForm(name = "Legolas", level = "7", maxHp = "31")
            viewModel.updateParticipant()

            assertEquals(
                listOf(
                    participant(name = "Aragorn", level = 4, maxHp = 30),
                    participant(name = "Legolas", level = 7, maxHp = 31),
                    participant(name = "Gimli", level = 5, maxHp = 35)
                ),
                viewModel.participantsUiState.value.participants
            )
        }

    @Test
    fun updateParticipant_onSuccess_clearsEditForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(stored = listOf(participant(name = "Aragorn")))

            viewModel.editParticipant(0)
            viewModel.fillForm(name = "Strider", level = "4", maxHp = "30")
            viewModel.updateParticipant()

            val state = viewModel.participantsUiState.value
            assertNull(state.editingIndex)
            assertFalse(state.isEditing)
            assertEquals("", state.participantName)
        }

    @Test
    fun updateParticipant_withInvalidInput_keepsEditingAndLeavesListUnchanged() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(participant(name = "Aragorn", level = 4, maxHp = 30))
            val viewModel = createViewModel(stored = stored)

            viewModel.editParticipant(0)
            viewModel.fillForm(name = "Aragorn", level = "0", maxHp = "30")
            viewModel.updateParticipant()

            val state = viewModel.participantsUiState.value
            assertEquals(stored, state.participants)
            assertEquals(0, state.editingIndex)
            assertNotNull(state.error)
        }

    @Test
    fun updateParticipant_withoutActiveEdit_doesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(participant(name = "Aragorn", level = 4, maxHp = 30))
            val viewModel = createViewModel(stored = stored)

            viewModel.fillForm(name = "Impostor", level = "9", maxHp = "99")
            viewModel.updateParticipant()

            assertEquals(stored, viewModel.participantsUiState.value.participants)
        }

    @Test
    fun cancelEdit_clearsFormAndEditingIndex() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(participant(name = "Aragorn", level = 4, maxHp = 30))
            val viewModel = createViewModel(stored = stored)

            viewModel.editParticipant(0)
            viewModel.cancelEdit()

            val state = viewModel.participantsUiState.value
            assertNull(state.editingIndex)
            assertEquals("", state.participantName)
            assertEquals("", state.participantLevel)
            assertEquals("", state.participantMaxHp)
            assertEquals(stored, state.participants)
        }

    // -------------------------------------------------------------- removal

    @Test
    fun removeParticipant_removesOnlyTheGivenIndex() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn"),
                    participant(name = "Legolas"),
                    participant(name = "Gimli")
                )
            )

            viewModel.removeParticipant(1)

            assertEquals(
                listOf(participant(name = "Aragorn"), participant(name = "Gimli")),
                viewModel.participantsUiState.value.participants
            )
        }

    /**
     * Regression guard: removal used to be `list - participant`, which threw away every
     * structurally equal row instead of the single one the user tapped.
     */
    @Test
    fun removeParticipant_withDuplicates_removesOnlyOneRow() =
        runTest(mainDispatcherRule.testDispatcher) {

            val twin = participant(name = "Clone", level = 2, maxHp = 12)
            val viewModel = createViewModel(stored = listOf(twin, twin, twin))

            viewModel.removeParticipant(1)

            assertEquals(listOf(twin, twin), viewModel.participantsUiState.value.participants)
        }

    @Test
    fun removeParticipant_withOutOfBoundsIndex_changesNothing() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(participant(name = "Aragorn"))
            val viewModel = createViewModel(stored = stored)

            viewModel.removeParticipant(3)

            assertEquals(stored, viewModel.participantsUiState.value.participants)
        }

    @Test
    fun removeParticipant_beforeEditedIndex_shiftsEditingIndexDown() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn"),
                    participant(name = "Legolas"),
                    participant(name = "Gimli")
                )
            )

            viewModel.editParticipant(2)
            viewModel.removeParticipant(0)

            val state = viewModel.participantsUiState.value
            assertEquals(1, state.editingIndex)
            // The form still edits Gimli, who is now the second row.
            assertEquals("Gimli", state.participantName)
            assertEquals("Gimli", state.participants[1].name)
        }

    @Test
    fun removeParticipant_afterEditedIndex_keepsEditingIndex() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn"),
                    participant(name = "Legolas"),
                    participant(name = "Gimli")
                )
            )

            viewModel.editParticipant(0)
            viewModel.removeParticipant(2)

            val state = viewModel.participantsUiState.value
            assertEquals(0, state.editingIndex)
            assertEquals("Aragorn", state.participantName)
        }

    @Test
    fun removeParticipant_atEditedIndex_clearsEditForm() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(
                stored = listOf(
                    participant(name = "Aragorn"),
                    participant(name = "Legolas")
                )
            )

            viewModel.editParticipant(1)
            viewModel.removeParticipant(1)

            val state = viewModel.participantsUiState.value
            assertNull(state.editingIndex)
            assertEquals("", state.participantName)
            assertEquals("", state.participantLevel)
            assertEquals("", state.participantMaxHp)
        }

    // ------------------------------------------------------ unsaved changes

    @Test
    fun loadedParticipants_startWithoutUnsavedChanges() =
        runTest(mainDispatcherRule.testDispatcher) {

            val stored = listOf(participant(name = "Aragorn"))
            val viewModel = createViewModel(stored = stored)

            val state = viewModel.participantsUiState.value
            assertEquals(stored, state.participants)
            assertEquals(stored, state.savedParticipants)
            assertFalse(state.hasUnsavedChanges)
        }

    @Test
    fun addParticipant_marksChangesAsUnsaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")

            viewModel.addParticipant()

            assertTrue(viewModel.participantsUiState.value.hasUnsavedChanges)
        }

    @Test
    fun saveParticipants_marksChangesAsSaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")
            viewModel.addParticipant()

            viewModel.saveParticipants()
            advanceUntilIdle()

            val state = viewModel.participantsUiState.value
            assertFalse(state.hasUnsavedChanges)
            assertFalse(state.isSaving)
            assertNull(state.error)
        }

    @Test
    fun saveParticipants_sendsCurrentCollectionToRepository() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")
            viewModel.addParticipant()

            viewModel.saveParticipants()
            advanceUntilIdle()

            assertEquals(
                FakeCampaignRepository.UpdateParticipantsCall(
                    campaignId = CAMPAIGN_ID,
                    participants = listOf(
                        EventParticipant(name = "Gandalf", level = 5, maxHp = 42)
                    )
                ),
                campaignRepository.updateCampaignParticipantsCalls.single()
            )
        }

    @Test
    fun saveParticipants_whenRepositoryThrows_setsErrorAndClearsSaving() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.updateCampaignParticipantsError =
                IllegalStateException("participants not saved")

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")
            viewModel.addParticipant()

            viewModel.saveParticipants()
            advanceUntilIdle()

            val state = viewModel.participantsUiState.value
            assertEquals(UiText.Raw("participants not saved"), state.error)
            assertFalse(state.isSaving)
        }

    @Test
    fun saveParticipants_whenRepositoryThrows_keepsChangesUnsaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            campaignRepository.updateCampaignParticipantsError =
                IllegalStateException("participants not saved")

            val viewModel = createViewModel()
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")
            viewModel.addParticipant()

            viewModel.saveParticipants()
            advanceUntilIdle()

            assertTrue(viewModel.participantsUiState.value.hasUnsavedChanges)
        }

    /** The database must not yank edits out from under the user. */
    @Test
    fun databaseEmission_doesNotOverwriteUnsavedParticipantEdits() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(stored = listOf(participant(name = "Aragorn")))
            viewModel.fillForm(name = "Gandalf", level = "5", maxHp = "42")
            viewModel.addParticipant()

            val fromDatabase = listOf(participant(name = "Someone Else", level = 9, maxHp = 90))
            campaignRepository.campaignById.value =
                campaign(id = CAMPAIGN_ID, participants = fromDatabase)
            advanceUntilIdle()

            val state = viewModel.participantsUiState.value
            assertEquals(
                listOf(
                    participant(name = "Aragorn"),
                    EventParticipant(name = "Gandalf", level = 5, maxHp = 42)
                ),
                state.participants
            )
            assertEquals(fromDatabase, state.savedParticipants)
            assertTrue(state.hasUnsavedChanges)
        }

    @Test
    fun databaseEmission_replacesParticipantsWhenNothingIsUnsaved() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel(stored = listOf(participant(name = "Aragorn")))

            val fromDatabase = listOf(participant(name = "Someone Else", level = 9, maxHp = 90))
            campaignRepository.campaignById.value =
                campaign(id = CAMPAIGN_ID, participants = fromDatabase)
            advanceUntilIdle()

            val state = viewModel.participantsUiState.value
            assertEquals(fromDatabase, state.participants)
            assertEquals(fromDatabase, state.savedParticipants)
            assertFalse(state.hasUnsavedChanges)
        }

    private companion object {
        const val CAMPAIGN_ID = 42
    }
}
