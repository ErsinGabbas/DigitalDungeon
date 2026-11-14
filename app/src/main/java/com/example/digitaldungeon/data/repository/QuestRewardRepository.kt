package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import kotlinx.coroutines.flow.Flow

/**
 * The rewards of a quest.
 *
 * A repository of its own rather than more methods on [QuestRepository], the same way the
 * objectives are: the quest screen subscribes to each block independently, so one block failing to
 * load cannot take the quest down with it, and the rewards change for entirely different reasons
 * than the quest they hang off - the quest is rewritten while it is being planned, the rewards
 * while it is being settled.
 */
interface QuestRewardRepository {

    fun getRewardsForQuest(
        questId: Int
    ): Flow<List<QuestReward>>

    /**
     * Appends a reward to [questId].
     *
     * Appended and never placed, because rewards are a set: nothing is earned before anything
     * else, so there is no position to choose and no gesture to move one.
     *
     * [description] defaults to empty and [type] to [QuestRewardType.OTHER] so the cheapest thing
     * the DM can do mid-session is write a title and move on - a reward nobody has classified yet
     * is still better recorded than forgotten.
     */
    suspend fun addReward(
        questId: Int,
        title: String,
        description: String = "",
        type: QuestRewardType = QuestRewardType.OTHER
    )

    /**
     * Rewrites a reward.
     *
     * The write replaces the fields as a whole rather than patching the ones that changed, so a
     * caller editing only the title still hands back the description and the type as they were.
     * That is what the edit form already holds, and it keeps the stored row exactly what the form
     * showed - no field can quietly survive from a version of the reward the user was not looking
     * at.
     */
    suspend fun updateReward(
        id: Int,
        title: String,
        description: String,
        type: QuestRewardType
    )

    suspend fun deleteReward(
        id: Int
    )
}
