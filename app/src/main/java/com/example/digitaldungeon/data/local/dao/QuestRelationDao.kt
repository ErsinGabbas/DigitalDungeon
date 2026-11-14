package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestRelationEntity
import com.example.digitaldungeon.domain.model.QuestRelationType
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes links between quests.
 *
 * There is no update here. A relation has no editable field: its type and its two ends are the
 * whole of it, so changing any of them means deleting the row and adding a different one.
 */
@Dao
interface QuestRelationDao {

    /**
     * Everything attached to one quest, from either end.
     *
     * Both directions in one query, because a quest is as interested in what points at it as in
     * what it points at - the parent that owns it and the quest it unblocks are the same kind of
     * fact to whoever is reading the quest.
     */
    @Query(
        "SELECT * FROM quest_relations " +
            "WHERE sourceQuestId = :questId OR targetQuestId = :questId ORDER BY id ASC"
    )
    fun getRelationsForQuest(
        questId: Int
    ): Flow<List<QuestRelationEntity>>

    /**
     * Every relation of one type at once, which is how the parent chain gets walked when checking
     * whether a new parent link would close a cycle. A plain suspend read and not a [Flow]: the
     * answer is wanted once, before an insert, and a stream of it would keep arriving long after
     * the question stopped being asked.
     */
    @Query(
        "SELECT * FROM quest_relations WHERE relationType = :relationType"
    )
    suspend fun getRelationsOfType(
        relationType: QuestRelationType
    ): List<QuestRelationEntity>

    /**
     * Non-zero when this exact link exists already, which the caller reports as an outcome instead
     * of inserting a second row and letting the unique constraint throw.
     */
    @Query(
        "SELECT COUNT(*) FROM quest_relations WHERE sourceQuestId = :sourceQuestId " +
            "AND targetQuestId = :targetQuestId AND relationType = :relationType"
    )
    suspend fun countRelation(
        sourceQuestId: Int,
        targetQuestId: Int,
        relationType: QuestRelationType
    ): Int

    @Insert
    suspend fun insertRelation(
        relation: QuestRelationEntity
    )

    /**
     * Every relation in the database, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quest_relations ORDER BY sourceQuestId ASC, id ASC"
    )
    suspend fun getAllRelations(): List<QuestRelationEntity>

    @Insert
    suspend fun insertRelations(
        relations: List<QuestRelationEntity>
    )

    @Query(
        "DELETE FROM quest_relations"
    )
    suspend fun deleteAllRelations()

    @Query(
        "DELETE FROM quest_relations WHERE id = :id"
    )
    suspend fun deleteRelation(
        id: Int
    )
}
