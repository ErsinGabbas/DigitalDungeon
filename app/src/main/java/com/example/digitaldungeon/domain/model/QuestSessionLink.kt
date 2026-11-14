package com.example.digitaldungeon.domain.model

/**
 * A link saying that one quest came up in one session.
 *
 * That is the whole of it. There is no type and no direction, because a quest either featured in a
 * session or it did not; grading it further - "started here", "finished here" - would be a
 * judgement about play rather than a record of it, and a judgement is something the DM can write in
 * a note where it can be worded properly.
 *
 * It is a table of its own and not a `questId` column on `session_events`, for two reasons. The
 * same quest comes up across many sessions and one session touches many quests, so a single column
 * could only ever hold one end of that. And `session_events` is read every time a session's
 * timeline is drawn, which is the busiest read in the app - widening that row would make it pay for
 * a feature most of its readers never ask about.
 *
 * Both ends have to belong to the same campaign. A foreign key can say that a quest exists and that
 * a session exists but not that they belong together, so the repository checks it before writing,
 * which is the same rule [QuestRelation] lives under.
 *
 * The link carries no timestamp. When somebody recorded it says nothing anyone needs later, and the
 * session it points at already carries the date that does mean something.
 */
data class QuestSessionLink(
    val id: Int,
    val questId: Int,
    val sessionId: Int
)
