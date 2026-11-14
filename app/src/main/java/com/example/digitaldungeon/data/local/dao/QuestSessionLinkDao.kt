package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestSessionLinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes links between quests and sessions.
 *
 * There is no update here. A link has no editable field: its two ends are its identity, so changing
 * one of them means deleting the row and adding another rather than editing this one.
 */
@Dao
interface QuestSessionLinkDao {

    /**
     * The sessions one quest came up in, which is what the quest screen reads to show where the
     * quest has been played.
     */
    @Query(
        "SELECT * FROM quest_session_links WHERE questId = :questId ORDER BY id ASC"
    )
    fun getLinksForQuest(
        questId: Int
    ): Flow<List<QuestSessionLinkEntity>>

    /**
     * The quests one session touched, the same links read from the other end - which is why the
     * table carries an index on each of them.
     */
    @Query(
        "SELECT * FROM quest_session_links WHERE sessionId = :sessionId ORDER BY id ASC"
    )
    fun getLinksForSession(
        sessionId: Int
    ): Flow<List<QuestSessionLinkEntity>>

    /**
     * Non-zero when this exact link exists already, which the caller reports as an outcome instead
     * of inserting a second row and letting the unique constraint throw.
     */
    @Query(
        "SELECT COUNT(*) FROM quest_session_links WHERE questId = :questId " +
            "AND sessionId = :sessionId"
    )
    suspend fun countLink(
        questId: Int,
        sessionId: Int
    ): Int

    @Insert
    suspend fun insertLink(
        link: QuestSessionLinkEntity
    )

    @Query(
        "DELETE FROM quest_session_links WHERE id = :id"
    )
    suspend fun deleteLink(
        id: Int
    )

    /**
     * Every link in the database, for a full backup. Ordered so the produced document is stable:
     * the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quest_session_links ORDER BY questId ASC, id ASC"
    )
    suspend fun getAllLinks(): List<QuestSessionLinkEntity>

    @Insert
    suspend fun insertLinks(
        links: List<QuestSessionLinkEntity>
    )

    @Query(
        "DELETE FROM quest_session_links"
    )
    suspend fun deleteAllLinks()
}
