package com.example.digitaldungeon.ui.campaign.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.ui.campaign.state.CampaignDetailUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignParticipantsUiState
import com.example.digitaldungeon.ui.campaign.state.CampaignSessionsUiState
import com.example.digitaldungeon.ui.campaign.state.PendingSessionDeletion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * Comes from the `details/{campaignId}` route, which declares the argument as
     * [androidx.navigation.NavType.IntType] - a missing or malformed id is a
     * programming error, not a campaign number 0.
     */
    private val campaignId: Int = checkNotNull(savedStateHandle[CAMPAIGN_ID_KEY]) {
        "CampaignDetailViewModel requires a \"$CAMPAIGN_ID_KEY\" navigation argument"
    }

    // Campaign state - owns only campaign loading errors
    private val _campaignUiState =
        MutableStateFlow(CampaignDetailUiState(campaignId = campaignId))

    val campaignUiState: StateFlow<CampaignDetailUiState> =
        _campaignUiState.asStateFlow()

    // Session state - owns only session errors (load / add / delete)
    private val _sessionUiState =
        MutableStateFlow(CampaignSessionsUiState())

    val sessionUiState: StateFlow<CampaignSessionsUiState> =
        _sessionUiState.asStateFlow()

    // Participant management state - owns only participant form / saving errors
    private val _participantsUiState =
        MutableStateFlow(CampaignParticipantsUiState())

    val participantsUiState: StateFlow<CampaignParticipantsUiState> =
        _participantsUiState.asStateFlow()

    private var loadSessionsJob: Job? = null

    private var loadCampaignJob: Job? = null

    init {
        loadCampaign()
        loadSessions()
    }

    private fun loadCampaign() {

        loadCampaignJob?.cancel()

        loadCampaignJob = viewModelScope.launch {

            try {
                _campaignUiState.update { it.copy(isLoading = true) }
                campaignRepository
                    .getCampaignByIdFlow(campaignId)
                    .collect { campaign ->

                        onCampaignParticipantsLoaded(
                            campaign?.participants ?: emptyList()
                        )

                        _campaignUiState.update {
                            it.copy(
                                campaign = campaign,
                                isLoading = false,
                                error = if (campaign == null) {
                                    UiText.of(R.string.campaign_not_found)
                                } else {
                                    null
                                }
                            )
                        }
                    }
            } catch (e: Exception) {
                _campaignUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.campaign_error_load_one_failed)
                    )
                }
            }

        }
    }

    /**
     * Keeps the editable collection in sync with the database without throwing away
     * edits the user has not saved yet.
     */
    private fun onCampaignParticipantsLoaded(
        participants: List<EventParticipant>
    ) {

        _participantsUiState.update { current ->
            if (current.hasUnsavedChanges) {
                current.copy(savedParticipants = participants)
            } else {
                current.copy(
                    participants = participants,
                    savedParticipants = participants
                )
            }
        }
    }

    private fun loadSessions() {

        loadSessionsJob?.cancel()

        loadSessionsJob = viewModelScope.launch {

            try {
                _sessionUiState.update { it.copy(isLoading = true) }
                sessionRepository
                    .getSessionsForCampaign(campaignId)
                    .collect { sessions ->

                        _sessionUiState.update {
                            it.copy(
                                sessions = sessions,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.campaign_error_sessions_load_failed)
                    )
                }
            }
        }
    }

    fun onSessionTitleChange(
        value: String
    ) {

        _sessionUiState.update {
            it.copy(
                sessionTitle = value
            )
        }
    }

    fun onSessionNotesChange(
        value: String
    ) {

        _sessionUiState.update {
            it.copy(
                sessionNotes = value
            )
        }
    }

    fun addSession() {

        val title =
            _sessionUiState.value.sessionTitle

        val notes =
            _sessionUiState.value.sessionNotes

        if (title.isBlank()) {
            _sessionUiState.update {
                it.copy(error = UiText.of(R.string.campaign_session_title_required))
            }
            return
        }

        viewModelScope.launch {

            try {
                _sessionUiState.update { it.copy(isLoading = true) }
                sessionRepository.addSession(
                    campaignId = campaignId,
                    title = title,
                    notes = notes
                )

                _sessionUiState.update {
                    it.copy(
                        sessionTitle = "",
                        sessionNotes = "",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.campaign_error_session_add_failed)
                    )
                }
            }
        }
    }

    /**
     * Asks for confirmation instead of deleting straight away: a session takes all
     * of its events down with it.
     */
    fun requestDeleteSession(session: Session) {

        _sessionUiState.update {
            // Only one dialog at a time: asking to delete closes the edit dialog.
            it.clearEditDialog().copy(
                pendingDeletion = PendingSessionDeletion(
                    sessionId = session.id,
                    sessionTitle = session.title
                )
            )
        }
    }

    fun cancelDeleteSession() {

        _sessionUiState.update {
            it.copy(pendingDeletion = null)
        }
    }

    fun confirmDeleteSession() {

        val pending =
            _sessionUiState.value.pendingDeletion ?: return

        viewModelScope.launch {

            try {
                _sessionUiState.update {
                    it.copy(
                        pendingDeletion = null,
                        isLoading = true
                    )
                }
                sessionRepository.deleteSession(pending.sessionId)
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.campaign_error_session_delete_failed)
                    )
                }
            }

        }
    }

    /**
     * Opens the edit dialog on a copy of the stored title and notes, so the session
     * in the list is only touched once the user saves.
     */
    fun requestEditSession(session: Session) {

        _sessionUiState.update {
            it.copy(
                // Only one dialog at a time: editing closes a pending delete.
                pendingDeletion = null,
                editingSession = session,
                editSessionTitle = session.title,
                editSessionNotes = session.notes,
                editError = null
            )
        }
    }

    fun onEditSessionTitleChange(
        value: String
    ) {

        _sessionUiState.update {
            // Typing clears the rejection it caused.
            it.copy(
                editSessionTitle = value,
                editError = null
            )
        }
    }

    fun onEditSessionNotesChange(
        value: String
    ) {

        _sessionUiState.update {
            it.copy(
                editSessionNotes = value,
                editError = null
            )
        }
    }

    fun cancelEditSession() {

        _sessionUiState.update { it.clearEditDialog() }
    }

    fun confirmEditSession() {

        val editing =
            _sessionUiState.value.editingSession ?: return

        val title =
            _sessionUiState.value.editSessionTitle.trim()

        // Keeps the dialog open with the reason instead of closing it on a title
        // the session list could never show.
        if (title.isBlank()) {
            _sessionUiState.update {
                it.copy(editError = UiText.of(R.string.campaign_session_title_required))
            }
            return
        }

        val notes =
            _sessionUiState.value.editSessionNotes.trim()

        viewModelScope.launch {

            try {
                _sessionUiState.update {
                    it.clearEditDialog().copy(isLoading = true)
                }
                sessionRepository.updateSession(
                    id = editing.id,
                    title = title,
                    notes = notes
                )
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _sessionUiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.campaign_error_session_update_failed)
                    )
                }
            }
        }
    }

    private fun CampaignSessionsUiState.clearEditDialog() =
        copy(
            editingSession = null,
            editSessionTitle = "",
            editSessionNotes = "",
            editError = null
        )

    // Participant management methods
    fun onParticipantNameChange(value: String) {
        _participantsUiState.update {
            // Editing the form clears the rejection it caused.
            it.copy(participantName = value, error = null)
        }
    }

    fun onParticipantLevelChange(value: String) {
        _participantsUiState.update {
            it.copy(participantLevel = value, error = null)
        }
    }

    fun onParticipantMaxHpChange(value: String) {
        _participantsUiState.update {
            it.copy(participantMaxHp = value, error = null)
        }
    }

    fun addParticipant() {

        _participantsUiState.update { state ->

            when (val result = state.validateParticipantForm()) {

                is ParticipantFormResult.Invalid ->
                    state.copy(error = result.message)

                is ParticipantFormResult.Valid ->
                    state.copy(
                        participants = state.participants + result.participant,
                        error = null
                    ).clearForm()
            }
        }
    }

    fun editParticipant(index: Int) {

        _participantsUiState.update { state ->

            val participant =
                state.participants.getOrNull(index) ?: return@update state

            state.copy(
                editingIndex = index,
                participantName = participant.name,
                participantLevel = participant.level.toString(),
                participantMaxHp = participant.maxHp.toString(),
                error = null
            )
        }
    }

    fun updateParticipant() {

        _participantsUiState.update { state ->

            val index =
                state.editingIndex ?: return@update state

            if (index !in state.participants.indices) return@update state

            when (val result = state.validateParticipantForm()) {

                is ParticipantFormResult.Invalid ->
                    state.copy(error = result.message)

                is ParticipantFormResult.Valid ->
                    state.copy(
                        participants = state.participants.mapIndexed { i, existing ->
                            if (i == index) result.participant else existing
                        },
                        error = null
                    ).clearForm()
            }
        }
    }

    fun removeParticipant(index: Int) {

        _participantsUiState.update { state ->

            if (index !in state.participants.indices) return@update state

            val updated =
                state.participants.filterIndexed { i, _ -> i != index }

            val editingIndex = state.editingIndex

            if (editingIndex == null) {
                state.copy(participants = updated)
            } else if (editingIndex == index) {
                // The row being edited is gone - drop the edit form as well.
                state.copy(participants = updated).clearForm()
            } else {
                // Keep editing the same row, its position may have shifted.
                state.copy(
                    participants = updated,
                    editingIndex = if (index < editingIndex) editingIndex - 1 else editingIndex
                )
            }
        }
    }

    fun cancelEdit() {
        _participantsUiState.update { it.clearForm().copy(error = null) }
    }

    fun saveParticipants() {

        viewModelScope.launch {

            val participants =
                _participantsUiState.value.participants

            try {
                _participantsUiState.update {
                    it.copy(isSaving = true)
                }

                campaignRepository.updateCampaignParticipants(
                    campaignId = campaignId,
                    participants = participants
                )

                // getCampaignByIdFlow pushes the stored value back, this only marks
                // the collection as clean right away.
                _participantsUiState.update {
                    it.copy(
                        savedParticipants = participants,
                        isSaving = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _participantsUiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.asUiText(R.string.campaign_error_participants_save_failed)
                    )
                }
            }
        }
    }

    /**
     * Rejects what the database would otherwise happily store: blank names and
     * non-positive or absurd levels and hit points. The reason is handed back so
     * the screen can show it instead of a button that silently does nothing.
     */
    private fun CampaignParticipantsUiState.validateParticipantForm(): ParticipantFormResult {

        val name = participantName.trim()

        if (name.isBlank()) {
            return ParticipantFormResult.Invalid(
                UiText.of(R.string.campaign_participant_name_required)
            )
        }

        val level = participantLevel.trim().toIntOrNull()
            ?: return ParticipantFormResult.Invalid(
                UiText.of(R.string.campaign_participant_level_not_a_number)
            )

        if (level !in MIN_LEVEL..MAX_LEVEL) {
            // The bounds are arguments and not part of the sentence: a
            // translation decides where the numbers go in it.
            return ParticipantFormResult.Invalid(
                UiText.of(
                    R.string.campaign_participant_level_out_of_range,
                    MIN_LEVEL,
                    MAX_LEVEL
                )
            )
        }

        val maxHp = participantMaxHp.trim().toIntOrNull()
            ?: return ParticipantFormResult.Invalid(
                UiText.of(R.string.campaign_participant_max_hp_not_a_number)
            )

        if (maxHp !in MIN_MAX_HP..MAX_MAX_HP) {
            return ParticipantFormResult.Invalid(
                UiText.of(
                    R.string.campaign_participant_max_hp_out_of_range,
                    MIN_MAX_HP,
                    MAX_MAX_HP
                )
            )
        }

        return ParticipantFormResult.Valid(
            EventParticipant(
                name = name,
                level = level,
                maxHp = maxHp
            )
        )
    }

    private fun CampaignParticipantsUiState.clearForm() =
        copy(
            participantName = "",
            participantLevel = "",
            participantMaxHp = "",
            editingIndex = null
        )

    companion object {

        const val CAMPAIGN_ID_KEY = "campaignId"

        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 100

        const val MIN_MAX_HP = 1
        const val MAX_MAX_HP = 9999
    }
}

/**
 * Outcome of validating the participant form: either a participant to store or
 * the reason the input was refused.
 */
private sealed interface ParticipantFormResult {

    data class Valid(val participant: EventParticipant) : ParticipantFormResult

    /** The reason, still unformatted - see [UiText]. */
    data class Invalid(val message: UiText) : ParticipantFormResult
}
