package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.repository.QuestConsequenceRepository
import com.example.digitaldungeon.data.repository.AddSessionLinkResult
import com.example.digitaldungeon.data.repository.QuestHistoryRepository
import com.example.digitaldungeon.data.repository.QuestNoteRepository
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.data.repository.QuestObjectiveRepository
import com.example.digitaldungeon.data.repository.AddRelationResult
import com.example.digitaldungeon.data.repository.QuestRelationRepository
import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.data.repository.QuestRewardRepository
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestRelationType
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.PendingConsequenceDeletion
import com.example.digitaldungeon.ui.quest.state.PendingNoteDeletion
import com.example.digitaldungeon.ui.quest.state.PendingObjectiveDeletion
import com.example.digitaldungeon.ui.quest.state.PendingRelationDeletion
import com.example.digitaldungeon.ui.quest.state.PendingRewardDeletion
import com.example.digitaldungeon.ui.quest.state.PendingSessionLinkDeletion
import com.example.digitaldungeon.ui.quest.state.QuestConsequencesUiState
import com.example.digitaldungeon.ui.quest.state.QuestDetailUiState
import com.example.digitaldungeon.ui.quest.state.QuestNotesUiState
import com.example.digitaldungeon.ui.quest.state.QuestObjectivesUiState
import com.example.digitaldungeon.ui.quest.state.QuestLink
import com.example.digitaldungeon.ui.quest.state.QuestLinkKind
import com.example.digitaldungeon.ui.quest.state.QuestRelationsUiState
import com.example.digitaldungeon.ui.quest.state.QuestHistoryUiState
import com.example.digitaldungeon.ui.quest.state.QuestRewardsUiState
import com.example.digitaldungeon.ui.quest.state.QuestSessionEntry
import com.example.digitaldungeon.ui.quest.state.QuestSessionsUiState
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One quest and everything hanging off it, read from the id the route carried.
 *
 * All of it is observed rather than fetched once, so a change made here is reflected by the same
 * flows that drew the screen - there is no second copy of anything to keep in sync.
 *
 * One state flow per block and not one for the screen, for the reason the campaign screen has
 * three: the quest, its steps, its rewards, its consequences, its notes and its links fail
 * independently, and a block that would not load must not wipe out "quest not found" - nor
 * the other way round. It also keeps a keystroke in one block's form from recomposing the five
 * blocks around it.
 *
 * The cost is admitted rather than hidden: six flows and five sets of form handlers make this
 * the longest ViewModel in the app by a wide margin. The alternative - one state object for the
 * whole screen - would merge six error channels into one and make every keystroke recompose
 * everything, which is a worse trade. If a seventh block is ever added, this is the file to
 * split, not the design to abandon.
 */
@HiltViewModel
class QuestDetailViewModel @Inject constructor(
    private val repository: QuestRepository,
    private val objectiveRepository: QuestObjectiveRepository,
    private val rewardRepository: QuestRewardRepository,
    private val consequenceRepository: QuestConsequenceRepository,
    private val noteRepository: QuestNoteRepository,
    private val relationRepository: QuestRelationRepository,
    private val historyRepository: QuestHistoryRepository,
    private val sessionLinkRepository: QuestSessionLinkRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val questId: Int = checkNotNull(savedStateHandle[QUEST_ID_KEY]) {
        "QuestDetailViewModel requires a \"$QUEST_ID_KEY\" navigation argument"
    }

    private val _uiState = MutableStateFlow(QuestDetailUiState(questId = questId))
    val uiState: StateFlow<QuestDetailUiState> = _uiState.asStateFlow()

    private val _objectivesUiState = MutableStateFlow(QuestObjectivesUiState())
    val objectivesUiState: StateFlow<QuestObjectivesUiState> = _objectivesUiState.asStateFlow()

    private val _rewardsUiState = MutableStateFlow(QuestRewardsUiState())
    val rewardsUiState: StateFlow<QuestRewardsUiState> = _rewardsUiState.asStateFlow()

    private val _consequencesUiState = MutableStateFlow(QuestConsequencesUiState())
    val consequencesUiState: StateFlow<QuestConsequencesUiState> =
        _consequencesUiState.asStateFlow()

    private val _notesUiState = MutableStateFlow(QuestNotesUiState())
    val notesUiState: StateFlow<QuestNotesUiState> = _notesUiState.asStateFlow()

    private val _relationsUiState = MutableStateFlow(QuestRelationsUiState(questId = questId))
    val relationsUiState: StateFlow<QuestRelationsUiState> = _relationsUiState.asStateFlow()

    private val _historyUiState = MutableStateFlow(QuestHistoryUiState())
    val historyUiState: StateFlow<QuestHistoryUiState> = _historyUiState.asStateFlow()

    private val _sessionsUiState = MutableStateFlow(QuestSessionsUiState())
    val sessionsUiState: StateFlow<QuestSessionsUiState> = _sessionsUiState.asStateFlow()

    /** Which campaign [candidateQuestsJob] and [candidateSessionsJob] are following, if any. */
    private var observedCampaignId: Int? = null

    private var candidateQuestsJob: Job? = null

    private var candidateSessionsJob: Job? = null

    init {
        loadQuest()
        loadObjectives()
        loadRewards()
        loadConsequences()
        loadNotes()
        loadRelations()
        loadHistory()
        loadSessionLinks()
    }

    private fun loadQuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getQuestById(questId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_load_one_failed)
                        )
                    }
                }
                .collect { quest ->
                    // quest == null is a valid state - the quest was deleted - so loading has to
                    // stop on the very first emission either way.
                    _uiState.update {
                        it.copy(
                            quest = quest,
                            isLoading = false
                        )
                    }

                    // The quests a link may point at hang off the campaign, which is only
                    // reachable through the quest - so this cannot be asked for in init.
                    onCampaignIdResolved(quest?.campaignId)
                }
        }
    }

    /**
     * Moves the quest to [status].
     *
     * Writes straight through without asking: the status is the one field this screen owns, every
     * direction is allowed, and a confirmation step on something that is this easy to put back
     * would only be in the way. A status equal to the current one is dropped - it is what tapping
     * the already selected chip does, and it has nothing to write.
     */
    fun onStatusSelected(status: QuestStatus) {
        val current = _uiState.value.quest ?: return
        if (current.status == status) return

        viewModelScope.launch {
            try {
                repository.updateQuestStatus(
                    id = current.id,
                    status = status
                )

                // A write that landed takes the previous failure off the screen: a message about
                // something that has since worked is worse than no message.
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_status_failed))
                }
            }
        }
    }

    // -------------------------
    // OBJECTIVES
    // -------------------------

    private fun loadObjectives() {
        viewModelScope.launch {
            _objectivesUiState.update { it.copy(isLoading = true) }

            objectiveRepository.getObjectivesForQuest(questId)
                .catch { throwable ->
                    _objectivesUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(
                                R.string.quest_error_objectives_load_failed
                            )
                        )
                    }
                }
                .collect { objectives ->
                    // A quest with no steps is a complete quest, so loading has to stop on the
                    // very first emission either way. The error is cleared here as well: the next
                    // write that lands makes this flow emit again, and a message about something
                    // that has since worked is worse than no message.
                    _objectivesUiState.update {
                        it.copy(
                            objectives = objectives,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    fun onObjectiveTitleChange(value: String) {
        // Typing clears the rejection it caused.
        _objectivesUiState.update { it.copy(formTitle = value, formError = null) }
    }

    fun onObjectiveDescriptionChange(value: String) {
        _objectivesUiState.update { it.copy(formDescription = value) }
    }

    fun onObjectiveOptionalChange(isOptional: Boolean) {
        _objectivesUiState.update { it.copy(formIsOptional = isOptional) }
    }

    /**
     * Appends what the form holds.
     *
     * Says why instead of silently doing nothing on an empty step, like every other form in this
     * app. Trimming happens here and not as the user types, so a title can still be written with
     * a trailing space without the field fighting back.
     */
    fun onAddObjective() {
        val state = _objectivesUiState.value
        val title = state.formTitle.trim()

        if (title.isBlank()) {
            _objectivesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_objective_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val isOptional = state.formIsOptional

        viewModelScope.launch {
            try {
                objectiveRepository.addObjective(
                    questId = questId,
                    title = title,
                    description = description,
                    isOptional = isOptional
                )

                _objectivesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _objectivesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_objective_add_failed))
                }
            }
        }
    }

    /**
     * Loads an objective into the form.
     *
     * Opening the form on a copy of the stored values, so the row in the list is only touched once
     * the user saves. Asking to edit also takes a pending deletion off the screen - one question
     * at a time.
     */
    fun onEditObjectiveRequest(objective: QuestObjective) {
        _objectivesUiState.update {
            it.copy(
                pendingDeletion = null,
                editingObjectiveId = objective.id,
                formTitle = objective.title,
                formDescription = objective.description,
                formIsOptional = objective.isOptional,
                formError = null
            )
        }
    }

    fun onCancelObjectiveEdit() {
        _objectivesUiState.update { it.clearForm() }
    }

    fun onUpdateObjective() {
        val state = _objectivesUiState.value
        val editingId = state.editingObjectiveId ?: return

        val title = state.formTitle.trim()
        if (title.isBlank()) {
            _objectivesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_objective_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val isOptional = state.formIsOptional

        viewModelScope.launch {
            try {
                objectiveRepository.updateObjective(
                    id = editingId,
                    title = title,
                    description = description,
                    isOptional = isOptional
                )

                _objectivesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _objectivesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_objective_update_failed))
                }
            }
        }
    }

    /**
     * Moves one step to [status]. Tapping the chip it already has writes nothing.
     */
    fun onObjectiveStatusSelected(
        objective: QuestObjective,
        status: QuestObjectiveStatus
    ) {
        if (objective.status == status) return

        viewModelScope.launch {
            try {
                objectiveRepository.updateObjectiveStatus(
                    id = objective.id,
                    status = status
                )
            } catch (e: Exception) {
                _objectivesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_objective_status_failed))
                }
            }
        }
    }

    fun onMoveObjectiveUp(objective: QuestObjective) {
        moveObjective(objective, QuestObjectiveRepository.MoveDirection.UP)
    }

    fun onMoveObjectiveDown(objective: QuestObjective) {
        moveObjective(objective, QuestObjectiveRepository.MoveDirection.DOWN)
    }

    /**
     * Shifts [objective] one position within its quest.
     *
     * The edges are already disabled on the row; this guard is what keeps the ViewModel honest if
     * the list changed between composition and the tap.
     */
    private fun moveObjective(
        objective: QuestObjective,
        direction: QuestObjectiveRepository.MoveDirection
    ) {
        val objectives = _objectivesUiState.value.objectives

        val index = objectives.indexOfFirst { it.id == objective.id }
        if (index == -1) return

        val targetIndex = if (direction == QuestObjectiveRepository.MoveDirection.UP) {
            index - 1
        } else {
            index + 1
        }
        if (targetIndex !in objectives.indices) return

        viewModelScope.launch {
            try {
                objectiveRepository.moveObjective(
                    id = objective.id,
                    direction = direction
                )
            } catch (e: Exception) {
                _objectivesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_objective_move_failed))
                }
            }
        }
    }

    /** Arms the delete confirmation. Nothing is removed until [onConfirmObjectiveDelete]. */
    fun onObjectiveDeleteRequest(objective: QuestObjective) {
        _objectivesUiState.update {
            it.copy(
                pendingDeletion = PendingObjectiveDeletion(
                    objectiveId = objective.id,
                    objectiveTitle = objective.title
                )
            )
        }
    }

    fun onCancelObjectiveDelete() {
        _objectivesUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmObjectiveDelete() {
        val pending = _objectivesUiState.value.pendingDeletion ?: return

        _objectivesUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                objectiveRepository.deleteObjective(pending.objectiveId)

                // The form may have been editing exactly the step that is now gone; leaving it
                // open would offer to save a row that no longer exists.
                if (_objectivesUiState.value.editingObjectiveId == pending.objectiveId) {
                    _objectivesUiState.update { it.clearForm() }
                }
            } catch (e: Exception) {
                _objectivesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_objective_delete_failed))
                }
            }
        }
    }

    private fun QuestObjectivesUiState.clearForm(): QuestObjectivesUiState =
        copy(
            editingObjectiveId = null,
            formTitle = "",
            formDescription = "",
            formIsOptional = false,
            formError = null
        )

    // -------------------------
    // REWARDS
    // -------------------------

    private fun loadRewards() {
        viewModelScope.launch {
            _rewardsUiState.update { it.copy(isLoading = true) }

            rewardRepository.getRewardsForQuest(questId)
                .catch { throwable ->
                    _rewardsUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_rewards_load_failed)
                        )
                    }
                }
                .collect { rewards ->
                    _rewardsUiState.update {
                        it.copy(rewards = rewards, isLoading = false, error = null)
                    }
                }
        }
    }

    fun onRewardTitleChange(value: String) {
        // Typing clears the rejection it caused.
        _rewardsUiState.update { it.copy(formTitle = value, formError = null) }
    }

    fun onRewardDescriptionChange(value: String) {
        _rewardsUiState.update { it.copy(formDescription = value) }
    }

    fun onRewardTypeSelected(type: QuestRewardType) {
        _rewardsUiState.update { it.copy(formType = type) }
    }

    fun onAddReward() {
        val state = _rewardsUiState.value
        val title = state.formTitle.trim()

        if (title.isBlank()) {
            _rewardsUiState.update {
                it.copy(formError = UiText.of(R.string.quest_reward_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val type = state.formType

        viewModelScope.launch {
            try {
                rewardRepository.addReward(
                    questId = questId,
                    title = title,
                    description = description,
                    type = type
                )

                _rewardsUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _rewardsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_reward_add_failed))
                }
            }
        }
    }

    /** Opens the form on a copy of the stored values; the row is only touched once saved. */
    fun onEditRewardRequest(reward: QuestReward) {
        _rewardsUiState.update {
            it.copy(
                // One question at a time: asking to edit takes a pending deletion off the screen.
                pendingDeletion = null,
                editingRewardId = reward.id,
                formTitle = reward.title,
                formDescription = reward.description,
                formType = reward.type,
                formError = null
            )
        }
    }

    fun onCancelRewardEdit() {
        _rewardsUiState.update { it.clearForm() }
    }

    fun onUpdateReward() {
        val state = _rewardsUiState.value
        val editingId = state.editingRewardId ?: return

        val title = state.formTitle.trim()
        if (title.isBlank()) {
            _rewardsUiState.update {
                it.copy(formError = UiText.of(R.string.quest_reward_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val type = state.formType

        viewModelScope.launch {
            try {
                rewardRepository.updateReward(
                    id = editingId,
                    title = title,
                    description = description,
                    type = type
                )

                _rewardsUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _rewardsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_reward_update_failed))
                }
            }
        }
    }

    fun onRewardDeleteRequest(reward: QuestReward) {
        _rewardsUiState.update {
            it.copy(
                pendingDeletion = PendingRewardDeletion(
                    rewardId = reward.id,
                    rewardTitle = reward.title
                )
            )
        }
    }

    fun onCancelRewardDelete() {
        _rewardsUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmRewardDelete() {
        val pending = _rewardsUiState.value.pendingDeletion ?: return

        _rewardsUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                rewardRepository.deleteReward(pending.rewardId)

                // The form may have been editing exactly the row that is now gone.
                if (_rewardsUiState.value.editingRewardId == pending.rewardId) {
                    _rewardsUiState.update { it.clearForm() }
                }
            } catch (e: Exception) {
                _rewardsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_reward_delete_failed))
                }
            }
        }
    }

    private fun QuestRewardsUiState.clearForm(): QuestRewardsUiState =
        copy(
            editingRewardId = null,
            formTitle = "",
            formDescription = "",
            formType = QuestRewardType.OTHER,
            formError = null
        )

    // -------------------------
    // CONSEQUENCES
    // -------------------------

    private fun loadConsequences() {
        viewModelScope.launch {
            _consequencesUiState.update { it.copy(isLoading = true) }

            consequenceRepository.getConsequencesForQuest(questId)
                .catch { throwable ->
                    _consequencesUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(
                                R.string.quest_error_consequences_load_failed
                            )
                        )
                    }
                }
                .collect { consequences ->
                    _consequencesUiState.update {
                        it.copy(consequences = consequences, isLoading = false, error = null)
                    }
                }
        }
    }

    fun onConsequenceTitleChange(value: String) {
        _consequencesUiState.update { it.copy(formTitle = value, formError = null) }
    }

    fun onConsequenceDescriptionChange(value: String) {
        _consequencesUiState.update { it.copy(formDescription = value) }
    }

    fun onConsequenceTypeSelected(type: QuestConsequenceType) {
        _consequencesUiState.update { it.copy(formType = type) }
    }

    fun onAddConsequence() {
        val state = _consequencesUiState.value
        val title = state.formTitle.trim()

        if (title.isBlank()) {
            _consequencesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_consequence_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val type = state.formType

        viewModelScope.launch {
            try {
                consequenceRepository.addConsequence(
                    questId = questId,
                    title = title,
                    description = description,
                    type = type
                )

                _consequencesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _consequencesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_consequence_add_failed))
                }
            }
        }
    }

    fun onEditConsequenceRequest(consequence: QuestConsequence) {
        _consequencesUiState.update {
            it.copy(
                pendingDeletion = null,
                editingConsequenceId = consequence.id,
                formTitle = consequence.title,
                formDescription = consequence.description,
                formType = consequence.type,
                formError = null
            )
        }
    }

    fun onCancelConsequenceEdit() {
        _consequencesUiState.update { it.clearForm() }
    }

    fun onUpdateConsequence() {
        val state = _consequencesUiState.value
        val editingId = state.editingConsequenceId ?: return

        val title = state.formTitle.trim()
        if (title.isBlank()) {
            _consequencesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_consequence_title_required))
            }
            return
        }

        val description = state.formDescription.trim()
        val type = state.formType

        viewModelScope.launch {
            try {
                consequenceRepository.updateConsequence(
                    id = editingId,
                    title = title,
                    description = description,
                    type = type
                )

                _consequencesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _consequencesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_consequence_update_failed))
                }
            }
        }
    }

    fun onConsequenceDeleteRequest(consequence: QuestConsequence) {
        _consequencesUiState.update {
            it.copy(
                pendingDeletion = PendingConsequenceDeletion(
                    consequenceId = consequence.id,
                    consequenceTitle = consequence.title
                )
            )
        }
    }

    fun onCancelConsequenceDelete() {
        _consequencesUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmConsequenceDelete() {
        val pending = _consequencesUiState.value.pendingDeletion ?: return

        _consequencesUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                consequenceRepository.deleteConsequence(pending.consequenceId)

                if (_consequencesUiState.value.editingConsequenceId == pending.consequenceId) {
                    _consequencesUiState.update { it.clearForm() }
                }
            } catch (e: Exception) {
                _consequencesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_consequence_delete_failed))
                }
            }
        }
    }

    private fun QuestConsequencesUiState.clearForm(): QuestConsequencesUiState =
        copy(
            editingConsequenceId = null,
            formTitle = "",
            formDescription = "",
            formType = QuestConsequenceType.OTHER,
            formError = null
        )

    // -------------------------
    // NOTES
    // -------------------------

    private fun loadNotes() {
        viewModelScope.launch {
            _notesUiState.update { it.copy(isLoading = true) }

            noteRepository.getNotesForQuest(questId)
                .catch { throwable ->
                    _notesUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_notes_load_failed)
                        )
                    }
                }
                .collect { notes ->
                    _notesUiState.update {
                        it.copy(notes = notes, isLoading = false, error = null)
                    }
                }
        }
    }

    fun onNoteContentChange(value: String) {
        _notesUiState.update { it.copy(formContent = value, formError = null) }
    }

    fun onAddNote() {
        val content = _notesUiState.value.formContent.trim()

        if (content.isBlank()) {
            _notesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_note_content_required))
            }
            return
        }

        viewModelScope.launch {
            try {
                // The timestamps are the repository's: a note written now must not be able to
                // claim it was written earlier.
                noteRepository.addNote(questId = questId, content = content)

                _notesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _notesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_note_add_failed))
                }
            }
        }
    }

    fun onEditNoteRequest(note: QuestNote) {
        _notesUiState.update {
            it.copy(
                pendingDeletion = null,
                editingNoteId = note.id,
                formContent = note.content,
                formError = null
            )
        }
    }

    fun onCancelNoteEdit() {
        _notesUiState.update { it.clearForm() }
    }

    fun onUpdateNote() {
        val state = _notesUiState.value
        val editingId = state.editingNoteId ?: return

        val content = state.formContent.trim()
        if (content.isBlank()) {
            _notesUiState.update {
                it.copy(formError = UiText.of(R.string.quest_note_content_required))
            }
            return
        }

        viewModelScope.launch {
            try {
                noteRepository.updateNote(id = editingId, content = content)

                _notesUiState.update { it.clearForm() }
            } catch (e: Exception) {
                _notesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_note_update_failed))
                }
            }
        }
    }

    fun onNoteDeleteRequest(note: QuestNote) {
        _notesUiState.update {
            it.copy(pendingDeletion = PendingNoteDeletion(noteId = note.id))
        }
    }

    fun onCancelNoteDelete() {
        _notesUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmNoteDelete() {
        val pending = _notesUiState.value.pendingDeletion ?: return

        _notesUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                noteRepository.deleteNote(pending.noteId)

                if (_notesUiState.value.editingNoteId == pending.noteId) {
                    _notesUiState.update { it.clearForm() }
                }
            } catch (e: Exception) {
                _notesUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_note_delete_failed))
                }
            }
        }
    }

    private fun QuestNotesUiState.clearForm(): QuestNotesUiState =
        copy(
            editingNoteId = null,
            formContent = "",
            formError = null
        )

    // -------------------------
    // LINKS BETWEEN QUESTS
    // -------------------------

    private fun loadRelations() {
        viewModelScope.launch {
            _relationsUiState.update { it.copy(isLoading = true) }

            relationRepository.getRelationsForQuest(questId)
                .catch { throwable ->
                    _relationsUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_relations_load_failed)
                        )
                    }
                }
                .collect { relations ->
                    _relationsUiState.update {
                        it.copy(relations = relations, isLoading = false, error = null)
                    }
                }
        }
    }

    /**
     * Re-points the candidate subscription when the quest finally names its campaign, or names a
     * different one.
     *
     * Guarded by [observedCampaignId] so that an ordinary status change - which makes the quest
     * flow emit again with the same campaign - does not restart the query. The same shape the
     * session screen uses to follow its campaign's roster.
     */
    private fun onCampaignIdResolved(campaignId: Int?) {
        if (campaignId == observedCampaignId) return

        observedCampaignId = campaignId
        candidateQuestsJob?.cancel()
        candidateSessionsJob?.cancel()

        if (campaignId == null) {
            _relationsUiState.update { it.copy(candidates = emptyList()) }
            _sessionsUiState.update { it.copy(candidates = emptyList()) }
            return
        }

        candidateSessionsJob = viewModelScope.launch {
            sessionRepository.getSessionsForCampaign(campaignId)
                .catch { throwable ->
                    _sessionsUiState.update {
                        it.copy(
                            error = throwable.asUiText(
                                R.string.quest_error_session_links_load_failed
                            )
                        )
                    }
                }
                .collect { sessions ->
                    _sessionsUiState.update { state ->
                        state.copy(
                            candidates = sessions,
                            // A pick must not outlive the session it named.
                            selectedSessionId = state.selectedSessionId
                                ?.takeIf { picked -> sessions.any { it.id == picked } }
                        )
                    }
                }
        }

        candidateQuestsJob = viewModelScope.launch {
            repository.getQuestsForCampaign(campaignId)
                .catch { throwable ->
                    _relationsUiState.update {
                        it.copy(
                            error = throwable.asUiText(R.string.quest_error_relations_load_failed)
                        )
                    }
                }
                .collect { quests ->
                    // This quest is not a candidate for a link to itself; the repository would
                    // refuse it anyway, and offering it would be offering a dead end.
                    _relationsUiState.update { state ->
                        state.copy(
                            candidates = quests.filterNot { it.id == questId },
                            // A pick must not outlive the quest it named: whoever was deleted
                            // drops out of the selection too, rather than leaving a button that
                            // refers to nothing.
                            selectedTargetId = state.selectedTargetId
                                ?.takeIf { picked -> quests.any { it.id == picked } }
                        )
                    }
                }
        }
    }

    fun onRelationKindSelected(kind: QuestLinkKind) {
        // Changing what the link would say clears the refusal the previous attempt earned.
        _relationsUiState.update { it.copy(selectedKind = kind, formError = null) }
    }

    fun onRelationTargetSelected(questId: Int) {
        _relationsUiState.update { it.copy(selectedTargetId = questId, formError = null) }
    }

    /**
     * Writes the link the picker describes.
     *
     * The six sentences the user picks between collapse onto four stored types and a direction.
     * Five of them are expressed by handing the repository this quest as the source;
     * [QuestLinkKind.CHILD_OF] is the exception and swaps the ends here, because
     * `PARENT_CHILD` has no named inverse to hand over - unlike `FOLLOWS`, which the repository
     * rewrites itself.
     *
     * Every refusal comes back as a value and lands next to the form. None of them is a failure:
     * they are the ordinary outcomes of picking the wrong quest out of a list.
     */
    fun onAddRelation() {
        val state = _relationsUiState.value

        val targetId = state.selectedTargetId
        if (targetId == null) {
            _relationsUiState.update {
                it.copy(formError = UiText.of(R.string.quest_relation_target_required))
            }
            return
        }

        val source: Int
        val target: Int
        val type: QuestRelationType

        when (state.selectedKind) {
            QuestLinkKind.PARENT_OF -> {
                source = questId; target = targetId; type = QuestRelationType.PARENT_CHILD
            }

            QuestLinkKind.CHILD_OF -> {
                source = targetId; target = questId; type = QuestRelationType.PARENT_CHILD
            }

            QuestLinkKind.PRECEDES -> {
                source = questId; target = targetId; type = QuestRelationType.PRECEDES
            }

            QuestLinkKind.FOLLOWS -> {
                source = questId; target = targetId; type = QuestRelationType.FOLLOWS
            }

            QuestLinkKind.ALTERNATIVE -> {
                source = questId; target = targetId; type = QuestRelationType.ALTERNATIVE
            }

            QuestLinkKind.RELATED -> {
                source = questId; target = targetId; type = QuestRelationType.RELATED
            }
        }

        viewModelScope.launch {
            try {
                val result = relationRepository.addRelation(
                    sourceQuestId = source,
                    targetQuestId = target,
                    relationType = type
                )

                _relationsUiState.update { current ->
                    when (result) {
                        // The picked quest is cleared so the next link starts from nothing; the
                        // kind is left alone, because adding three children in a row is the
                        // normal way this screen is used.
                        AddRelationResult.Added -> current.copy(
                            selectedTargetId = null,
                            formError = null
                        )

                        else -> current.copy(formError = result.asFormError())
                    }
                }
            } catch (e: Exception) {
                _relationsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_relation_add_failed))
                }
            }
        }
    }

    fun onRelationDeleteRequest(link: QuestLink) {
        _relationsUiState.update {
            it.copy(
                pendingDeletion = PendingRelationDeletion(
                    relationId = link.relation.id,
                    // Falls back to the same sentence the row shows when the far end is not in the
                    // loaded list, so the question still names something.
                    otherQuestTitle = link.otherQuestTitle
                        ?.let { UiText.Raw(it) }
                        ?: UiText.of(R.string.quest_relation_unknown_quest)
                )
            )
        }
    }

    fun onCancelRelationDelete() {
        _relationsUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmRelationDelete() {
        val pending = _relationsUiState.value.pendingDeletion ?: return

        _relationsUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                relationRepository.deleteRelation(pending.relationId)
            } catch (e: Exception) {
                _relationsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_relation_delete_failed))
                }
            }
        }
    }

    /**
     * The sentence a refusal reads as.
     *
     * One line per outcome, because they are different things to be told: a user shown "that link
     * already exists" when the real reason was a cycle learns nothing about what to do next.
     * [AddRelationResult.Added] has no message, and asking for one is a programming error.
     */
    private fun AddRelationResult.asFormError(): UiText =
        when (this) {
            AddRelationResult.AlreadyExists ->
                UiText.of(R.string.quest_relation_refused_exists)

            AddRelationResult.SameQuest ->
                UiText.of(R.string.quest_relation_refused_same_quest)

            AddRelationResult.DifferentCampaigns ->
                UiText.of(R.string.quest_relation_refused_other_campaign)

            AddRelationResult.WouldCreateCycle ->
                UiText.of(R.string.quest_relation_refused_cycle)

            AddRelationResult.QuestMissing ->
                UiText.of(R.string.quest_relation_refused_missing)

            AddRelationResult.Added ->
                error("Added is not a refusal and has no message")
        }

    // -------------------------
    // HISTORY
    //
    // Read-only: nothing here writes an entry. They are appended by the repositories that perform
    // the change, inside the same transaction as the change - see QuestHistoryRepository.
    // -------------------------

    private fun loadHistory() {
        viewModelScope.launch {
            _historyUiState.update { it.copy(isLoading = true) }

            historyRepository.getHistoryForQuest(questId)
                .catch { throwable ->
                    _historyUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_history_load_failed)
                        )
                    }
                }
                .collect { entries ->
                    _historyUiState.update {
                        it.copy(entries = entries, isLoading = false, error = null)
                    }
                }
        }
    }

    // -------------------------
    // SESSIONS THE QUEST CAME UP IN
    // -------------------------

    private fun loadSessionLinks() {
        viewModelScope.launch {
            _sessionsUiState.update { it.copy(isLoading = true) }

            sessionLinkRepository.getLinksForQuest(questId)
                .catch { throwable ->
                    _sessionsUiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(
                                R.string.quest_error_session_links_load_failed
                            )
                        )
                    }
                }
                .collect { links ->
                    _sessionsUiState.update {
                        it.copy(links = links, isLoading = false, error = null)
                    }
                }
        }
    }

    fun onSessionSelected(sessionId: Int) {
        _sessionsUiState.update { it.copy(selectedSessionId = sessionId, formError = null) }
    }

    /**
     * Records that this quest came up in the picked session.
     *
     * Every refusal comes back as a value and lands next to the picker, for the same reason linking
     * two quests does: choosing the wrong session out of a list is an ordinary thing to do.
     */
    fun onAddSessionLink() {
        val sessionId = _sessionsUiState.value.selectedSessionId

        if (sessionId == null) {
            _sessionsUiState.update {
                it.copy(formError = UiText.of(R.string.quest_session_required))
            }
            return
        }

        viewModelScope.launch {
            try {
                val result = sessionLinkRepository.addLink(
                    questId = questId,
                    sessionId = sessionId
                )

                _sessionsUiState.update { current ->
                    when (result) {
                        AddSessionLinkResult.Added -> current.copy(
                            selectedSessionId = null,
                            formError = null
                        )

                        else -> current.copy(formError = result.asFormError())
                    }
                }
            } catch (e: Exception) {
                _sessionsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_session_link_add_failed))
                }
            }
        }
    }

    fun onSessionLinkDeleteRequest(entry: QuestSessionEntry) {
        _sessionsUiState.update {
            it.copy(
                pendingDeletion = PendingSessionLinkDeletion(
                    linkId = entry.link.id,
                    sessionTitle = entry.sessionTitle
                        ?.let { title -> UiText.Raw(title) }
                        ?: UiText.of(R.string.quest_session_unknown)
                )
            )
        }
    }

    fun onCancelSessionLinkDelete() {
        _sessionsUiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmSessionLinkDelete() {
        val pending = _sessionsUiState.value.pendingDeletion ?: return

        _sessionsUiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                sessionLinkRepository.deleteLink(pending.linkId)
            } catch (e: Exception) {
                _sessionsUiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_session_link_delete_failed))
                }
            }
        }
    }

    /** One line per refusal, for the reason spelled out on `AddRelationResult.asFormError`. */
    private fun AddSessionLinkResult.asFormError(): UiText =
        when (this) {
            AddSessionLinkResult.AlreadyExists ->
                UiText.of(R.string.quest_session_refused_exists)

            AddSessionLinkResult.DifferentCampaigns ->
                UiText.of(R.string.quest_session_refused_other_campaign)

            AddSessionLinkResult.QuestMissing ->
                UiText.of(R.string.quest_session_refused_quest_missing)

            AddSessionLinkResult.SessionMissing ->
                UiText.of(R.string.quest_session_refused_session_missing)

            AddSessionLinkResult.Added ->
                error("Added is not a refusal and has no message")
        }

    companion object {

        /** Name of the `quest/{questId}` route argument. Shared with the tests. */
        const val QUEST_ID_KEY = "questId"
    }
}
