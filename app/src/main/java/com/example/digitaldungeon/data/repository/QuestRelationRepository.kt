package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestRelation
import com.example.digitaldungeon.domain.model.QuestRelationType
import kotlinx.coroutines.flow.Flow

/**
 * The links between quests.
 *
 * Its own repository, like every other block of the quest screen, so that a failure to load the
 * links cannot take the quest down with it.
 *
 * This is the one repository in the app that enforces rules of its own, because three of them
 * cannot be written into the schema and the fourth is only half there:
 *
 * A quest may not be linked to itself; both quests must belong to the same campaign, which a
 * foreign key cannot express; a parent link may not close a cycle, which no constraint can see;
 * and a duplicate is caught by a unique index *and* checked beforehand, so that it comes back as
 * an answer rather than as an exception.
 *
 * Because those refusals are ordinary outcomes of a user picking the wrong quest from a list -
 * not failures - [addRelation] reports them as values. Nothing is thrown for them, the same way
 * `BackupService.restore` returns a `RestoreResult` instead of throwing at a file it does not like.
 */
interface QuestRelationRepository {

    /**
     * Every link that touches [questId], in either direction.
     *
     * One flow and not two, because a quest is as interested in what points at it as in what it
     * points at: the screen groups the rows itself once it knows which end it is looking from.
     */
    fun getRelationsForQuest(
        questId: Int
    ): Flow<List<QuestRelation>>

    /**
     * Links [sourceQuestId] to [targetQuestId], after normalising the pair and refusing the four
     * things that must never be stored.
     *
     * Normalisation is why the row that lands may not be the row that was asked for.
     * [QuestRelationType.FOLLOWS] is rewritten as [QuestRelationType.PRECEDES] with the ends
     * swapped, and the two symmetric types are stored with the smaller id first - so "A follows B"
     * and "B precedes A" become the same row, and so do "A is related to B" and "B is related to
     * A". Two rows stating one fact is how a journal ends up disagreeing with itself.
     *
     * The whole check-then-write runs in a single transaction: a rule that is verified and then
     * written separately is a rule that holds only when nobody else is writing.
     */
    suspend fun addRelation(
        sourceQuestId: Int,
        targetQuestId: Int,
        relationType: QuestRelationType
    ): AddRelationResult

    /**
     * Removes one link. Neither quest is touched - that is the point of a link being its own row.
     */
    suspend fun deleteRelation(
        id: Int
    )
}

/**
 * What came of asking for a link.
 *
 * A sealed type rather than a boolean or an exception: every one of these is a sentence the screen
 * has to be able to say, and they are different sentences. "That quest is in another campaign" and
 * "that would make the quest its own ancestor" are both refusals, but a user who sees the wrong one
 * learns nothing.
 */
sealed interface AddRelationResult {

    /** The link was written. */
    data object Added : AddRelationResult

    /** That exact link already exists, possibly written the other way round and normalised. */
    data object AlreadyExists : AddRelationResult

    /** Both ends were the same quest. */
    data object SameQuest : AddRelationResult

    /**
     * The two quests belong to different campaigns.
     *
     * The rule that keeps two campaigns' journals apart; without it a link would be the one way to
     * walk from one campaign into another.
     */
    data object DifferentCampaigns : AddRelationResult

    /**
     * The parent link would make a quest its own ancestor, directly or through a chain of other
     * parent links.
     */
    data object WouldCreateCycle : AddRelationResult

    /** One of the two quests no longer exists - deleted while the picker was open. */
    data object QuestMissing : AddRelationResult
}
