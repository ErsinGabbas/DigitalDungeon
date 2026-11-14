package com.example.digitaldungeon.domain.model

/**
 * One thing the quest left behind: the dead guard captain, the faction that will not trade with the
 * party any more, the village that is ash now.
 *
 * Separate from [Quest.description] because the two answer different questions and change at
 * different times. The description is what the quest is about and is written before anything
 * happens; a consequence is what came of it and is written afterwards, often much later and often
 * more than once. A quest also outlives its own closing through these: once it is done the quest
 * itself stops being interesting, but the burnt village is still there three sessions on and the
 * DM needs to find it without reopening a finished quest and reading its prose for clues.
 *
 * What a consequence weighs is left as prose for the same reason a reward's worth is: "the
 * harbourmaster's widow blames the party" has no scale to put it on, so [description] carries
 * whatever the DM wants to remember and [type] only says which part of the world moved.
 *
 * Deliberately no orderIndex, like [QuestReward] and unlike [QuestObjective]. Consequences are a
 * set and not a sequence - nothing here follows from anything else in the list, they all simply
 * happened - and the list is append-only, so `ORDER BY id` is already a stable and reproducible
 * order: the same rows always come out the same way, with no position for the DM to maintain and no
 * reorder gesture to support.
 */
data class QuestConsequence(
    val id: Int,
    val questId: Int,
    val title: String,
    val description: String,
    val type: QuestConsequenceType
)

/**
 * Which part of the world this consequence touched: a change in the world itself, the fate of an
 * NPC, how a faction now stands towards the party, or anything else.
 *
 * Four constants and not a free-form tag, because the type exists to group the list and pick an
 * icon on the quest screen, and a tag the user invents would do neither. [OTHER] is what keeps the
 * list honest: a consequence that fits none of the three still goes in, said in the DM's own words,
 * rather than being pushed into the nearest wrong drawer.
 *
 * Room stores these as TEXT by constant name, so renaming one makes already stored rows unreadable
 * - see the keep rule in `proguard-rules.pro`, which pins the names against minification for the
 * same reason. The declaration order is the order the UI offers them in.
 */
enum class QuestConsequenceType {
    WORLD,
    NPC,
    FACTION,
    OTHER
}
