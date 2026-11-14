package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.ui.text.UiText

/**
 * State of the rewards block on the quest screen.
 *
 * Its own state object, like the objectives block, so that a failure to load the rewards cannot
 * overwrite - or be overwritten by - the quest's own error or any other block's. By the end of the
 * quest tracker this screen carries several of these; keeping them apart is what lets each message
 * sit next to the thing it is about.
 *
 * @param editingRewardId the reward the form is currently rewriting, `null` while it is adding a
 *   new one. One form serves both, like the participant editor on the campaign screen.
 * @param formError why the form refused to save - the reward had no title. Separate from [error],
 *   which is about a write that failed: one is the user's to fix, the other is not.
 */
data class QuestRewardsUiState(
    val rewards: List<QuestReward> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val editingRewardId: Int? = null,
    val formTitle: String = "",
    val formDescription: String = "",
    val formType: QuestRewardType = QuestRewardType.OTHER,
    val formError: UiText? = null,
    val pendingDeletion: PendingRewardDeletion? = null
) {

    /** True while the form is rewriting an existing reward rather than adding one. */
    val isEditing: Boolean
        get() = editingRewardId != null

    /**
     * Nothing has been written down yet. A normal state - plenty of quests are their own reason -
     * so it is said out loud rather than left as blank space.
     */
    val hasNoRewards: Boolean
        get() = rewards.isEmpty() && !isLoading
}
