package com.example.digitaldungeon.ui.quest.state

/**
 * The order the quest list is read in.
 *
 * A UI concept and not a domain one: it is a decision about one screen's current view, it is not
 * stored anywhere, and the database knows nothing about it - which is also why it cannot be an
 * argument of the repository.
 */
enum class QuestSort {

    /**
     * Most important first, newest first within the same priority.
     *
     * The default, because a quest tracker is read to answer "what is in front of the party now",
     * and the creation date only answers "what did I write down last".
     */
    PRIORITY,

    /** Newest first, which is the order the quests arrive in from the database. */
    NEWEST
}
