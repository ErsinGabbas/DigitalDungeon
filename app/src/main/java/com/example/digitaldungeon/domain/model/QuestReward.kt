package com.example.digitaldungeon.domain.model

/**
 * One thing the party gets for a quest: the purse, the favour, the sword at the end of it.
 *
 * A quest needs none of these - plenty of quests are their own reason - and a quest may promise
 * several at once, which is why this is a list and not a field on [Quest]. What a reward is worth
 * is left as prose: an amount column would have to be a number in some unit, and "a minor noble
 * owes you a favour" has no unit, so [description] carries whatever the DM wants to remember and
 * [type] only says which drawer it belongs in.
 *
 * Deliberately no orderIndex, unlike [QuestObjective]. Rewards are a set and not a sequence -
 * nothing is earned before anything else - and the list is append-only, so `ORDER BY id` already
 * gives a stable and reproducible order for free: the same rows always come out the same way, and
 * there is no position for the DM to maintain or for a reorder gesture to shift.
 */
data class QuestReward(
    val id: Int,
    val questId: Int,
    val title: String,
    val description: String,
    val type: QuestRewardType
)

/**
 * Which kind of reward this is.
 *
 * A short closed list rather than free-form tags, because the point of the type is telling a purse
 * from a favour at a glance, and a tag the user invents would not do that for anyone but them.
 * [OTHER] is what makes the list survivable: anything that does not fit still goes in, named in its
 * own words, instead of being forced into the nearest wrong drawer.
 *
 * Room stores these as TEXT by constant name, so a rename breaks already stored rows - see the keep
 * rule in `proguard-rules.pro`, which pins the names against minification for the same reason. The
 * declaration order is the order the UI offers them in.
 */
enum class QuestRewardType {
    EXPERIENCE,
    MONEY,
    ITEM,
    REPUTATION,
    OTHER
}
