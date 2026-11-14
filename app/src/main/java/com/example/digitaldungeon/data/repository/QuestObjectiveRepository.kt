package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import kotlinx.coroutines.flow.Flow

/**
 * The objectives of a quest.
 *
 * A repository of its own rather than more methods on [QuestRepository], the same way the session
 * timeline is separate from the sessions: the quest screen subscribes to the two independently, so
 * a failure to load the objectives cannot take the quest down with it, and the two lists change
 * for entirely different reasons.
 */
interface QuestObjectiveRepository {

    /** Direction in which an objective is shifted within its quest. */
    enum class MoveDirection {
        UP,
        DOWN
    }

    fun getObjectivesForQuest(
        questId: Int
    ): Flow<List<QuestObjective>>

    /**
     * Appends an objective to the end of the list of [questId].
     *
     * Always appended and never inserted in the middle: a step written down later is the next one
     * by default, and moving it is one gesture away. The position is taken from the stored maximum
     * rather than from the list the screen is showing, so two quick additions cannot land on the
     * same index.
     */
    suspend fun addObjective(
        questId: Int,
        title: String,
        description: String = "",
        isOptional: Boolean = false
    )

    /**
     * Rewrites what an objective says.
     *
     * Deliberately not its status and not its position: those are changed by their own gestures,
     * and a form that wrote them back would undo a tap made elsewhere on the screen.
     */
    suspend fun updateObjective(
        id: Int,
        title: String,
        description: String,
        isOptional: Boolean
    )

    /**
     * Moves an objective to [status].
     *
     * Any direction is allowed, including back out of [QuestObjectiveStatus.COMPLETED]: a step
     * ticked off by mistake has to be as easy to untick, and whether a step still counts is a
     * judgement about the story.
     */
    suspend fun updateObjectiveStatus(
        id: Int,
        status: QuestObjectiveStatus
    )

    /**
     * Swaps the objective with its neighbour above or below.
     *
     * Moving the first one [MoveDirection.UP] or the last one [MoveDirection.DOWN] (or passing an
     * unknown id) is a no-op.
     */
    suspend fun moveObjective(
        id: Int,
        direction: MoveDirection
    )

    suspend fun deleteObjective(
        id: Int
    )
}
