package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import kotlinx.coroutines.flow.Flow

/**
 * What a quest left behind in the world.
 *
 * A repository of its own rather than more methods on [QuestRepository], the same way the
 * objectives are: the quest screen subscribes to each block separately, so a failure to load the
 * consequences cannot take the quest down with it - the quest, its objectives and its rewards still
 * render while that one block reports its own trouble. The lists also change for entirely different
 * reasons, this one usually long after the quest itself was last touched.
 */
interface QuestConsequenceRepository {

    fun getConsequencesForQuest(
        questId: Int
    ): Flow<List<QuestConsequence>>

    /**
     * Appends a consequence to the list of [questId].
     *
     * Always appended, because the order is the order things happened and there is nowhere else to
     * put a new one. [description] and [type] are defaulted so the quick path - a title typed in
     * mid-session and nothing else - needs no decisions: the prose can be filled in afterwards, and
     * [QuestConsequenceType.OTHER] is an honest answer until the DM knows which part of the world
     * this really moved.
     */
    suspend fun addConsequence(
        questId: Int,
        title: String,
        description: String = "",
        type: QuestConsequenceType = QuestConsequenceType.OTHER
    )

    /**
     * Rewrites a consequence.
     *
     * Replaces the three fields as a whole rather than patching the ones that changed, because the
     * caller is a form that holds all three: a screen editing only the title still hands back the
     * description and the type it was showing, which keeps this method's result independent of what
     * the user happened to touch. Nothing else in the app writes to a consequence, so there is no
     * concurrent edit for a whole-field write to clobber.
     */
    suspend fun updateConsequence(
        id: Int,
        title: String,
        description: String,
        type: QuestConsequenceType
    )

    suspend fun deleteConsequence(
        id: Int
    )
}
