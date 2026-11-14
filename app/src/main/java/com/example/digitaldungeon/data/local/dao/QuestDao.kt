package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    /**
     * Every quest of one campaign, newest first.
     *
     * Ordered by [QuestEntity.createdAt] and not by priority, even though the list usually shows
     * priority order: priority is an enum stored as TEXT, so `ORDER BY priority` would sort it
     * alphabetically - HIGH, LOW, NORMAL - which is not an order at all. The real ordering is
     * applied to the loaded list, where the enum can be compared as the enum it is. What this
     * query owes the caller is a *stable* sequence to sort, and a timestamp gives that.
     */
    @Query(
        "SELECT * FROM quests WHERE campaignId = :campaignId ORDER BY createdAt DESC"
    )
    fun getQuestsForCampaign(
        campaignId: Int
    ): Flow<List<QuestEntity>>

    @Query(
        "SELECT * FROM quests WHERE id = :id"
    )
    fun getQuestById(
        id: Int
    ): Flow<QuestEntity?>

    /**
     * Which campaign a quest belongs to, or `null` if there is no such quest.
     *
     * Exists for the one rule the schema cannot state: a relation may only join two quests of the
     * same campaign. A foreign key can say "this quest exists"; it cannot say "and it is in the
     * same campaign as that other one", so the check is made before the row is written and this is
     * what it reads.
     */
    @Query(
        "SELECT campaignId FROM quests WHERE id = :id"
    )
    suspend fun getCampaignIdForQuest(id: Int): Int?

    @Insert
    suspend fun insertQuest(
        quest: QuestEntity
    )

    /**
     * Every quest of every campaign, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quests ORDER BY campaignId ASC, id ASC"
    )
    suspend fun getAllQuests(): List<QuestEntity>

    @Insert
    suspend fun insertQuests(
        quests: List<QuestEntity>
    )

    @Query(
        "DELETE FROM quests"
    )
    suspend fun deleteAllQuests()

    /**
     * Rewrites every field the edit form showed.
     *
     * [QuestEntity.createdAt] and [QuestEntity.campaignId] are deliberately absent: when a quest
     * was written down is a fact, and which campaign it belongs to is not something an edit may
     * change - moving a quest between campaigns would carry its objectives and relations across
     * the boundary that keeps two campaigns apart.
     */
    @Query(
        "UPDATE quests SET title = :title, description = :description, category = :category, " +
            "status = :status, priority = :priority, isHidden = :isHidden, " +
            "inGameDate = :inGameDate WHERE id = :id"
    )
    suspend fun updateQuest(
        id: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    )

    /**
     * Moves a quest to another status and touches nothing else.
     *
     * Separate from [updateQuest] because it is a different act, not a shorter one: the quest
     * screen changes the status in place, without having loaded the edit form, and it must not
     * write back six fields it never showed the user. It is also the single write that the quest
     * history will hang off later.
     */
    @Query(
        "UPDATE quests SET status = :status WHERE id = :id"
    )
    suspend fun updateQuestStatus(
        id: Int,
        status: QuestStatus
    )

    @Query(
        "DELETE FROM quests WHERE id = :id"
    )
    suspend fun deleteQuest(
        id: Int
    )
}
