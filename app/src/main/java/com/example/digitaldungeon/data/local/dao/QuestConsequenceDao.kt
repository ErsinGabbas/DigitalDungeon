package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestConsequenceEntity
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestConsequenceDao {

    /**
     * The consequences of one quest, oldest first.
     *
     * Ordered by id rather than by a stored position, because the list is append-only: the order
     * things were written down in is the order they happened in as far as the DM is concerned, and
     * it costs nothing to read it off the primary key. Spelled out in the query all the same - a
     * `SELECT` with no `ORDER BY` is whatever the query planner feels like, which is not something
     * a list on screen may depend on.
     */
    @Query(
        "SELECT * FROM quest_consequences WHERE questId = :questId ORDER BY id ASC"
    )
    fun getConsequencesForQuest(
        questId: Int
    ): Flow<List<QuestConsequenceEntity>>

    @Insert
    suspend fun insertConsequence(
        consequence: QuestConsequenceEntity
    )

    /**
     * Every consequence of every quest, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content, which is what lets a backup
     * be compared against another one instead of only restored.
     */
    @Query(
        "SELECT * FROM quest_consequences ORDER BY questId ASC, id ASC"
    )
    suspend fun getAllConsequences(): List<QuestConsequenceEntity>

    @Insert
    suspend fun insertConsequences(
        consequences: List<QuestConsequenceEntity>
    )

    @Query(
        "DELETE FROM quest_consequences"
    )
    suspend fun deleteAllConsequences()

    /**
     * Rewrites what the consequence says.
     *
     * All three fields at once, because all three are on the one form the DM edits; there is no
     * gesture anywhere else on the screen that touches a consequence, so nothing can be undone by
     * writing them back together.
     */
    @Query(
        "UPDATE quest_consequences SET title = :title, description = :description, " +
            "type = :type WHERE id = :id"
    )
    suspend fun updateConsequence(
        id: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    )

    @Query(
        "DELETE FROM quest_consequences WHERE id = :id"
    )
    suspend fun deleteConsequence(
        id: Int
    )
}
