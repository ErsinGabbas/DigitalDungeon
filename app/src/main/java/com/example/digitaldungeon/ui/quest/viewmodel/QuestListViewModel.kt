package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitaldungeon.R
import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.PendingQuestDeletion
import com.example.digitaldungeon.ui.quest.state.QuestListUiState
import com.example.digitaldungeon.ui.quest.state.QuestSort
import com.example.digitaldungeon.ui.text.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The quest list of one campaign.
 *
 * The campaign is fixed for the life of this ViewModel: it arrives through [SavedStateHandle] from
 * the route and is the only thing the repository is ever asked about, which is what keeps two
 * campaigns' quests from ever meeting on this screen.
 *
 * Searching, filtering and sorting are state changes and nothing else - no query is re-run for
 * them. The full list stays in [QuestListUiState.quests] and the view the screen draws is derived
 * from it, so "this campaign has no quests" never becomes indistinguishable from "nothing matched".
 */
@HiltViewModel
class QuestListViewModel @Inject constructor(
    private val repository: QuestRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * Comes from the `quests/{campaignId}` route, which declares the argument as
     * [androidx.navigation.NavType.IntType] - a missing or malformed id is a programming error,
     * not a campaign number 0.
     */
    private val campaignId: Int = checkNotNull(savedStateHandle[CAMPAIGN_ID_KEY]) {
        "QuestListViewModel requires a \"$CAMPAIGN_ID_KEY\" navigation argument"
    }

    private val _uiState = MutableStateFlow(QuestListUiState(campaignId = campaignId))
    val uiState: StateFlow<QuestListUiState> = _uiState.asStateFlow()

    init {
        loadQuests()
    }

    private fun loadQuests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getQuestsForCampaign(campaignId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.asUiText(R.string.quest_error_load_failed)
                        )
                    }
                }
                .collect { quests ->
                    // An empty list is a valid answer, so loading has to stop on the very first
                    // emission - otherwise a campaign without quests would spin forever.
                    //
                    // The error is cleared here, which is also how a failed delete stops being
                    // shown: the next write that does land makes this flow emit again, and a
                    // message about something that has since worked is worse than no message.
                    _uiState.update {
                        it.copy(
                            quests = quests,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    // -------------------------
    // SEARCH, FILTERS AND ORDER
    // -------------------------

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onClearSearchQuery() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    /** Passing `null` shows every status again. */
    fun onStatusFilterSelected(status: QuestStatus?) {
        _uiState.update { it.copy(statusFilter = status) }
    }

    /** Passing `null` shows every category again. */
    fun onCategoryFilterSelected(category: QuestCategory?) {
        _uiState.update { it.copy(categoryFilter = category) }
    }

    fun onSortSelected(sort: QuestSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    /**
     * Back to showing everything. The sort order is deliberately left alone: it hides nothing, so
     * it is not one of the things "reset filters" is about.
     */
    fun onClearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                statusFilter = null,
                categoryFilter = null
            )
        }
    }

    // -------------------------
    // DELETE
    // -------------------------

    /**
     * Arms the delete confirmation. Nothing is removed until [onConfirmDelete] is called.
     */
    fun onDeleteRequest(quest: Quest) {
        _uiState.update {
            it.copy(
                pendingDeletion = PendingQuestDeletion(
                    questId = quest.id,
                    questTitle = quest.title
                )
            )
        }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(pendingDeletion = null) }
    }

    fun onConfirmDelete() {
        val pending = _uiState.value.pendingDeletion ?: return

        _uiState.update { it.copy(pendingDeletion = null) }

        viewModelScope.launch {
            try {
                repository.deleteQuest(pending.questId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.asUiText(R.string.quest_error_delete_failed))
                }
            }
        }
    }

    companion object {

        /** Name of the `quests/{campaignId}` route argument. Shared with the tests. */
        const val CAMPAIGN_ID_KEY = "campaignId"
    }
}
