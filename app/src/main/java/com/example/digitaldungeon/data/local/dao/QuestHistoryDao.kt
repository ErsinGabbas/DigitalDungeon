package com.example.digitaldungeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads and appends; nothing else.
 *
 * There is no update and no delete-by-id here, and that absence is the whole design rather than
 * work left for later: append-only is not a promise made in a comment somewhere above the UI, it is
 * the fact that no statement exists anywhere in the app that could rewrite or drop a single history
 * row. The only delete is the table-wide one the restore path needs, and the only other way a row
 * disappears is the cascade from its quest.
 */
@Dao
interface QuestHistoryDao {

    /**
     * The history of one quest, newest first, because a log is read from the end: what the DM wants
     * on opening the screen is the last thing that happened, not the first.
     *
     * Tie-broken by id descending, so two entries written inside the same millisecond - a status
     * change and the objective change that prompted it, say - still come out in a fixed order
     * instead of whatever the query planner felt like, and that order is the one they were written
     * in, reversed along with everything else.
     */
    @Query(
        "SELECT * FROM quest_history WHERE questId = :questId ORDER BY timestamp DESC, id DESC"
    )
    fun getHistoryForQuest(
        questId: Int
    ): Flow<List<QuestHistoryEntity>>

    @Insert
    suspend fun insertEntry(
        entry: QuestHistoryEntity
    )

    /**
     * Every history row of every quest, for a full backup. Ordered so the produced document is
     * stable: the same database always exports byte-identical content.
     */
    @Query(
        "SELECT * FROM quest_history ORDER BY questId ASC, id ASC"
    )
    suspend fun getAllEntries(): List<QuestHistoryEntity>

    @Insert
    suspend fun insertEntries(
        entries: List<QuestHistoryEntity>
    )

    /**
     * Empties the table, and exists only for the restore path, which replaces the whole database in
     * one go and so has to clear what was there first.
     *
     * Not a way to clear one quest's history: there is deliberately nothing that takes a quest id,
     * because "wipe the log and start again" is exactly the rewrite an append-only history refuses.
     */
    @Query(
        "DELETE FROM quest_history"
    )
    suspend fun deleteAllEntries()
}
