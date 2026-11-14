package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * The log of what was done to a quest.
 *
 * Read-only on purpose, and the only repository in this app that is. Nothing the UI can call writes
 * a history row: the entries are appended by the repositories that perform the change, inside the
 * same transaction as the change itself. That is what makes the log trustworthy - a status that
 * moved without leaving an entry, or an entry without a status behind it, would both be worse than
 * no log at all, and neither is reachable if the only way to write one is to make the change.
 *
 * There is no way to edit or remove a single entry either. Rows leave only when the quest does, by
 * cascade.
 */
interface QuestHistoryRepository {

    /** The quest's log, newest first. */
    fun getHistoryForQuest(
        questId: Int
    ): Flow<List<QuestHistoryEntry>>
}
