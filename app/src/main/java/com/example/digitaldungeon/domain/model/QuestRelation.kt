package com.example.digitaldungeon.domain.model

/**
 * A link between two quests of the same campaign.
 *
 * One relation is one row joining two quests, and both of them stay quests in their own right: a
 * child keeps its own status, its own objectives and its own rewards, and is never a sub-item of
 * its parent. Nothing here nests - the link is drawn beside the quests, not around them, which is
 * what lets a quest belong to a chain without losing anything it had on its own.
 *
 * @param sourceQuestId the end the relation reads from. Direction carries meaning for
 *   [QuestRelationType.PARENT_CHILD], where the source is the parent and the target the child, and
 *   for [QuestRelationType.PRECEDES], where the source comes first. For
 *   [QuestRelationType.ALTERNATIVE] and [QuestRelationType.RELATED] it carries none, because those
 *   say the same thing read either way.
 */
data class QuestRelation(
    val id: Int,
    val sourceQuestId: Int,
    val targetQuestId: Int,
    val relationType: QuestRelationType
)

/**
 * What the link between two quests means.
 *
 * Five values the user can choose from and four that ever reach the table. [FOLLOWS] is the named
 * inverse of [PRECEDES] and exists so the user can say it in the direction they are thinking in,
 * but it is never stored: the repository rewrites `FOLLOWS(a, b)` as `PRECEDES(b, a)` on the way
 * in. Keeping both would let two rows state one fact, and two rows stating one fact disagree the
 * moment somebody edits one of them.
 *
 * Symmetric relations are normalised the same way and for the same reason - [ALTERNATIVE] and
 * [RELATED] are stored with the smaller quest id as the source, so "A is related to B" and "B is
 * related to A" are one row rather than two that have to be kept in step.
 *
 * Room stores these as TEXT by constant name, so a rename breaks already stored rows; the names
 * are pinned by a keep rule in `proguard-rules.pro`.
 */
enum class QuestRelationType {
    PARENT_CHILD,
    PRECEDES,
    FOLLOWS,
    ALTERNATIVE,
    RELATED
}
