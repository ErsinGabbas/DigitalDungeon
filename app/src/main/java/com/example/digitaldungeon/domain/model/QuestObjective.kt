package com.example.digitaldungeon.domain.model

/**
 * One step of a quest: what has to happen for it to move forward.
 *
 * A quest needs none of these - "the miller wants his daughter back" is a complete quest with no
 * objectives at all - and a quest may have one, a handful in sequence, or several that are open at
 * the same time. All three read the same way here, because what makes them sequential or parallel
 * is the story and not the data: the objectives are an ordered list, and whether the DM treats
 * items two and three as "next" or as "both now" is theirs to decide.
 *
 * Deliberately not a condition engine. There are no prerequisites, no automatic transitions and no
 * rule that completing the last objective completes the quest - see [QuestStatus].
 *
 * @param orderIndex position in the quest's list, dense and zero-based. A relation query gives no
 *   ordering guarantee of its own, and the order these are read in is the whole point of a list of
 *   steps, so it is stored rather than inferred.
 * @param isOptional a step that is worth doing but not required - the side room nobody has to
 *   open. Marked rather than hidden: an optional objective is still part of the quest.
 */
data class QuestObjective(
    val id: Int,
    val questId: Int,
    val title: String,
    val description: String,
    val status: QuestObjectiveStatus,
    val orderIndex: Int,
    val isOptional: Boolean
)

/**
 * Where one step stands.
 *
 * Three states and not the six of [QuestStatus], because a step is smaller than a quest: it has
 * not happened, it has happened, or it can no longer happen. There is no "paused" step - a quest
 * stalls, its steps simply wait - and no "cancelled" one, because a step that stopped mattering is
 * either failed or deleted.
 *
 * Changed by hand, and nothing is terminal: a step marked done by mistake goes back with one tap,
 * like a quest does. Stored as TEXT by constant name, like every other enum in this database.
 */
enum class QuestObjectiveStatus {
    NOT_STARTED,
    COMPLETED,
    FAILED
}
