package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.QuestEditUiState
import com.example.digitaldungeon.ui.text.UiText
import com.example.digitaldungeon.ui.text.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The form that writes a quest, in both of its modes.
 *
 * Which mode it is in is decided by the route that opened it and by nothing else: `quest/new/{id}`
 * carries a campaign and writes a new quest, `quest/{id}/edit` carries a quest and rewrites it.
 * Exactly one of the two arguments is ever present, which is why both are read as nullable and the
 * missing one is a programming error rather than a state.
 *
 * One ViewModel for both, because the two differ in three lines - where the initial values come
 * from and which repository call the save ends in - while the fields, the validation and the state
 * class are identical.
 */
@HiltViewModel
class QuestEditViewModel @Inject constructor(
    private val repository: QuestRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Set while editing an existing quest, `null` while writing a new one. */
    private val questId: Int? = savedStateHandle[QUEST_ID_KEY]

    /** Set while writing a new quest, `null` while editing one. */
    private val campaignId: Int? = savedStateHandle[CAMPAIGN_ID_KEY]

    private val _uiState = MutableStateFlow(QuestEditUiState())
    val uiState: StateFlow<QuestEditUiState> = _uiState.asStateFlow()

    init {
        if (questId != null) {
            loadQuest(questId)
        } else {
            checkNotNull(campaignId) {
                "QuestEditViewModel requires either a \"$QUEST_ID_KEY\" or a " +
                    "\"$CAMPAIGN_ID_KEY\" navigation argument"
            }
        }
    }

    /**
     * Reads the quest into the form once.
     *
     * Deliberately a single read off the flow and not a subscription: the form is a copy the user
     * is editing, so a later emission - from a status change made elsewhere, say - would overwrite
     * whatever they have typed since.
     */
    private fun loadQuest(id: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEditing = true,
                    isLoading = true,
                    // Unfolded while editing: folding away fields the quest already has values in
                    // would hide exactly what the user may have come to change.
                    areOptionalFieldsVisible = true
                )
            }

            try {
                val quest = repository.getQuestById(id).first()

                if (quest == null) {
                    // Deleted between the list and this form. Nothing to edit and nothing to
                    // write: an UPDATE would match no row and the form would report a save that
                    // never happened, so saving is refused from here on.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isMissing = true,
                            error = UiText.of(R.string.quest_not_found)
                        )
                    }
                    return@launch
                }

                _uiState.update { it.fromQuest(quest) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.asUiText(R.string.quest_error_load_one_failed)
                    )
                }
            }
        }
    }

    // -------------------------
    // FIELDS
    // -------------------------

    fun onTitleChange(value: String) {
        // Typing clears the rejection it caused.
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCategorySelected(category: QuestCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onStatusSelected(status: QuestStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun onPrioritySelected(priority: QuestPriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onHiddenChange(isHidden: Boolean) {
        _uiState.update { it.copy(isHidden = isHidden) }
    }

    fun onInGameDateChange(value: String) {
        _uiState.update { it.copy(inGameDate = value) }
    }

    fun onToggleOptionalFields() {
        _uiState.update { it.copy(areOptionalFieldsVisible = !it.areOptionalFieldsVisible) }
    }

    // -------------------------
    // SAVE
    // -------------------------

    /**
     * Writes the form, as a new quest or over the existing one.
     *
     * Says why instead of silently doing nothing when the title is empty, the same way adding a
     * campaign or a session does. The blanks are trimmed here and not as the user types, so a name
     * can still be typed with a trailing space without the field fighting back.
     */
    fun onSave() {
        val state = _uiState.value

        // A write already in flight, a form still loading, or a quest that no longer exists. The
        // same property the save button is enabled by, so the two cannot disagree.
        if (!state.isSaveEnabled) return

        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update {
                it.copy(titleError = UiText.of(R.string.quest_title_required))
            }
            return
        }

        val description = state.description.trim()
        // An empty field is not a date: the column is nullable precisely so that "no in-world
        // date" is stored as nothing rather than as an empty string.
        val inGameDate = state.inGameDate.trim().takeIf { it.isNotBlank() }

        // Marked as saving *before* the coroutine starts, not inside it. The flag is what the
        // guard above reads, and a second press arriving before the body had a chance to run
        // would otherwise write the same quest twice.
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                if (questId != null) {
                    repository.updateQuest(
                        id = questId,
                        title = title,
                        description = description,
                        category = state.category,
                        status = state.status,
                        priority = state.priority,
                        isHidden = state.isHidden,
                        inGameDate = inGameDate
                    )
                } else {
                    repository.addQuest(
                        campaignId = checkNotNull(campaignId),
                        title = title,
                        description = description,
                        category = state.category,
                        status = state.status,
                        priority = state.priority,
                        isHidden = state.isHidden,
                        inGameDate = inGameDate
                    )
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                val fallback = if (questId != null) {
                    R.string.quest_error_update_failed
                } else {
                    R.string.quest_error_add_failed
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.asUiText(fallback)
                    )
                }
            }
        }
    }

    private fun QuestEditUiState.fromQuest(quest: Quest): QuestEditUiState =
        copy(
            isEditing = true,
            title = quest.title,
            description = quest.description,
            category = quest.category,
            status = quest.status,
            priority = quest.priority,
            isHidden = quest.isHidden,
            // The form's field is a String; "no date" is the empty text.
            inGameDate = quest.inGameDate.orEmpty(),
            isLoading = false,
            isMissing = false,
            titleError = null,
            error = null
        )

    companion object {

        /** Name of the `quest/{questId}/edit` route argument. Shared with the tests. */
        const val QUEST_ID_KEY = "questId"

        /** Name of the `quest/new/{campaignId}` route argument. Shared with the tests. */
        const val CAMPAIGN_ID_KEY = "campaignId"
    }
}
