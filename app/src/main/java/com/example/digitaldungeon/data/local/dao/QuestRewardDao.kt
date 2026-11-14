package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestRewardEntity
import com.example.digitaldungeon.domain.model.QuestRewardType
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestRewardDao {

    /**
     * The rewards of one quest, oldest first.
     *
     * By [QuestRewardEntity.id] rather than by a stored position, because the table is append-only
     * and nothing reorders it: the key already encodes the order they were written down in, which
     * is a fixed order instead of whatever the query planner felt like, and it needs no column to
     * maintain.
     */
    @Query(
        "SELECT * FROM quest_rewards WHERE questId = :questId ORDER BY id ASC"
    )
    fun getRewardsForQuest(
        questId: Int
    ): Flow<List<QuestRewardEntity>>

    @Insert
    suspend fun insertReward(
        reward: QuestRewardEntity
    )

    /**
     * Every reward of every quest, for a full backup. Ordered so the produced document is stable:
     * the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quest_rewards ORDER BY questId ASC, id ASC"
    )
    suspend fun getAllRewards(): List<QuestRewardEntity>

    @Insert
    suspend fun insertRewards(
        rewards: List<QuestRewardEntity>
    )

    @Query(
        "DELETE FROM quest_rewards"
    )
    suspend fun deleteAllRewards()

    /**
     * Rewrites a reward as a whole. Every editable field is here because they are all edited in the
     * one form and there is no gesture that touches any of them on its own, so there is nothing a
     * write-back could undo - the reason `updateObjective` has to leave status and position alone.
     */
    @Query(
        "UPDATE quest_rewards SET title = :title, description = :description, " +
            "type = :type WHERE id = :id"
    )
    suspend fun updateReward(
        id: Int,
        title: String,
        description: String,
        type: QuestRewardType
    )

    @Query(
        "DELETE FROM quest_rewards WHERE id = :id"
    )
    suspend fun deleteReward(
        id: Int
    )
}
