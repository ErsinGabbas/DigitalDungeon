package com.example.digitaldungeon.domain.model

/**
 * One of the DM's running notes on a quest: whatever had to be remembered about it.
 *
 * Free text and nothing else. There is no title, because a note is a single thought and a heading
 * field would turn a one-line remark - "the miller is lying about the well" - into a form to fill
 * in; the first words of the content are already the best summary anybody could write. There is no
 * type either: sorting a thought into a category is work the DM would have to do at exactly the
 * moment they are trying to write something down quickly, and a note that was never filed is worth
 * more than a category that was never used.
 *
 * @param createdAt when the thought was written down, and it never changes afterwards. It is what
 *   the list is ordered by, so a note keeps its place even after being reworded.
 * @param updatedAt moves on every edit, so a note that was revised can be told apart from one that
 *   has stood untouched since it was first written - the two are equal exactly until the first
 *   edit.
 */
data class QuestNote(
    val id: Int,
    val questId: Int,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)
