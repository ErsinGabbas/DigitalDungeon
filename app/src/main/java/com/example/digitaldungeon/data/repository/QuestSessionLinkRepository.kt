package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestSessionLink
import kotlinx.coroutines.flow.Flow

/**
 * The record that a quest came up in a session.
 *
 * Read from both ends: the quest screen asks which sessions it featured in, the session screen asks
 * which quests it touched. One table serves both, and the link means the same thing from either
 * side - which is why there is no direction and no type here.
 *
 * Like [QuestRelationRepository], this enforces the rule the schema cannot: both ends must belong
 * to the same campaign. A foreign key can say the session exists; it cannot say it belongs to the
 * same campaign as the quest.
 */
interface QuestSessionLinkRepository {

    fun getLinksForQuest(
        questId: Int
    ): Flow<List<QuestSessionLink>>

    fun getLinksForSession(
        sessionId: Int
    ): Flow<List<QuestSessionLink>>

    /**
     * Records that [questId] came up in [sessionId].
     *
     * Refusals come back as values rather than exceptions, for the same reason they do when linking
     * two quests: picking the wrong session out of a list is an ordinary thing to do, not a failure.
     * The check and the write share one transaction.
     */
    suspend fun addLink(
        questId: Int,
        sessionId: Int
    ): AddSessionLinkResult

    /** Removes the record. Neither the quest nor the session is touched. */
    suspend fun deleteLink(
        id: Int
    )
}

/**
 * What came of recording that a quest featured in a session.
 *
 * A type of its own rather than a reuse of [AddRelationResult]: the cases are different ones, and a
 * shared type would carry a `WouldCreateCycle` that cannot happen here and lack the two "missing"
 * cases that can.
 */
sealed interface AddSessionLinkResult {

    data object Added : AddSessionLinkResult

    /** The quest is already recorded as having come up in that session. */
    data object AlreadyExists : AddSessionLinkResult

    /**
     * The session belongs to a different campaign than the quest.
     *
     * The rule that keeps two campaigns' journals apart - without it, a link would be a way to walk
     * from one campaign's quest into another's session.
     */
    data object DifferentCampaigns : AddSessionLinkResult

    /** The quest no longer exists - deleted while the picker was open. */
    data object QuestMissing : AddSessionLinkResult

    /** The session no longer exists. */
    data object SessionMissing : AddSessionLinkResult
}
