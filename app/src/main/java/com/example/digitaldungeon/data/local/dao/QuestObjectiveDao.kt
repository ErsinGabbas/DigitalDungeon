package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestObjectiveDao {

    /**
     * The objectives of one quest, in the order the DM put them in.
     *
     * Tie-broken by id, so two rows that somehow share an [QuestObjectiveEntity.orderIndex] still
     * come out in a fixed order instead of whatever the query planner felt like - the same
     * guarantee `getEventsForSessionSnapshot` makes about the timeline.
     */
    @Query(
        "SELECT * FROM quest_objectives WHERE questId = :questId ORDER BY orderIndex ASC, id ASC"
    )
    fun getObjectivesForQuest(
        questId: Int
    ): Flow<List<QuestObjectiveEntity>>

    /**
     * One objective by its id, or `null` if it is gone.
     *
     * Exists so that a status change can name the step it moved in the quest's history log: the log
     * stores the objective's title, and the title is not something the caller of
     * [updateObjectiveStatus] has to hand over - it is already in the row being changed.
     */
    @Query(
        "SELECT * FROM quest_objectives WHERE id = :id"
    )
    suspend fun getObjectiveById(id: Int): QuestObjectiveEntity?

    /** `null` for a quest with no objectives yet; the caller then starts at zero. */
    @Query(
        "SELECT MAX(orderIndex) FROM quest_objectives WHERE questId = :questId"
    )
    suspend fun getMaxOrderIndex(questId: Int): Int?

    @Insert
    suspend fun insertObjective(
        objective: QuestObjectiveEntity
    )

    /**
     * Every objective of every quest, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quest_objectives ORDER BY questId ASC, orderIndex ASC, id ASC"
    )
    suspend fun getAllObjectives(): List<QuestObjectiveEntity>

    @Insert
    suspend fun insertObjectives(
        objectives: List<QuestObjectiveEntity>
    )

    @Query(
        "DELETE FROM quest_objectives"
    )
    suspend fun deleteAllObjectives()

    /**
     * Rewrites what the objective says. Its status and its position are not here: both are
     * changed by their own gestures, and a form that rewrote them would undo a tap the user made
     * somewhere else.
     */
    @Query(
        "UPDATE quest_objectives SET title = :title, description = :description, " +
            "isOptional = :isOptional WHERE id = :id"
    )
    suspend fun updateObjective(
        id: Int,
        title: String,
        description: String,
        isOptional: Boolean
    )

    @Query(
        "UPDATE quest_objectives SET status = :status WHERE id = :id"
    )
    suspend fun updateObjectiveStatus(
        id: Int,
        status: QuestObjectiveStatus
    )

    @Query(
        "DELETE FROM quest_objectives WHERE id = :id"
    )
    suspend fun deleteObjective(
        id: Int
    )

    @Query(
        "SELECT questId FROM quest_objectives WHERE id = :objectiveId"
    )
    suspend fun getQuestIdForObjective(objectiveId: Int): Int?

    @Query(
        "SELECT * FROM quest_objectives WHERE questId = :questId ORDER BY orderIndex ASC, id ASC"
    )
    suspend fun getObjectivesForQuestSnapshot(questId: Int): List<QuestObjectiveEntity>

    @Query(
        "UPDATE quest_objectives SET orderIndex = :orderIndex WHERE id = :objectiveId"
    )
    suspend fun updateObjectiveOrderIndex(objectiveId: Int, orderIndex: Int)

    /**
     * Swaps an objective with its neighbour.
     *
     * The same recipe as `SessionEventDao.moveEvent`, and for the same reasons: it works off the
     * actual row order instead of arithmetic on [QuestObjectiveEntity.orderIndex], so the gaps a
     * deleted objective leaves behind do not matter, and the whole quest is renumbered to a dense
     * 0..n-1 sequence inside one transaction, which also heals those gaps. Degenerate cases - an
     * unknown objective, the first one moving up, the last one moving down - leave the list
     * untouched.
     */
    @Transaction
    suspend fun moveObjective(objectiveId: Int, moveUp: Boolean) {

        val questId = getQuestIdForObjective(objectiveId) ?: return

        val objectives = getObjectivesForQuestSnapshot(questId)

        val fromPosition = objectives.indexOfFirst { it.id == objectiveId }
        if (fromPosition == -1) return

        val toPosition = if (moveUp) fromPosition - 1 else fromPosition + 1
        if (toPosition !in objectives.indices) return

        val reordered = objectives.toMutableList()
        reordered[fromPosition] = objectives[toPosition]
        reordered[toPosition] = objectives[fromPosition]

        reordered.forEachIndexed { position, objective ->

            if (objective.orderIndex != position) {
                updateObjectiveOrderIndex(objective.id, position)
            }

        }
    }
}
