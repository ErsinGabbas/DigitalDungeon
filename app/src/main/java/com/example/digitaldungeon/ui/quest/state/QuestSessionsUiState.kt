package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.QuestSessionLink
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.ui.text.UiText

/**
 * One session a quest came up in, as the screen draws it: the stored row with the session named.
 *
 * @param sessionTitle `null` when the session is not among the loaded ones - the links and the
 *   campaign's sessions arrive on two flows, so for one frame a link can know an id whose title has
 *   not landed yet.
 */
data class QuestSessionEntry(
    val link: QuestSessionLink,
    val sessionId: Int,
    val sessionTitle: String?
)

/**
 * State of the "came up in" block on the quest screen.
 *
 * @param candidates the sessions of the same campaign, i.e. what the picker offers. Scoped to the
 *   campaign because that is the rule the repository enforces; offering a session it would refuse
 *   would be offering a dead end.
 * @param formError why the last attempt was refused - nothing was picked, or the repository said
 *   no. Separate from [error], which is a write that failed outright.
 */
data class QuestSessionsUiState(
    val links: List<QuestSessionLink> = emptyList(),
    val candidates: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val selectedSessionId: Int? = null,
    val formError: UiText? = null,
    val pendingDeletion: PendingSessionLinkDeletion? = null
) {

    /**
     * The links with their sessions named, newest session first.
     *
     * Derived rather than stored, like every other view in this app. Ordered by the session's own
     * date and not by the order the links were made: the question this block answers is "when did
     * this quest come up", and the session's date is the only thing that answers it.
     */
    val entries: List<QuestSessionEntry> = links
        .map { link ->
            val session = candidates.firstOrNull { it.id == link.sessionId }
            QuestSessionEntry(
                link = link,
                sessionId = link.sessionId,
                sessionTitle = session?.title
            )
        }
        .sortedByDescending { entry ->
            candidates.firstOrNull { it.id == entry.sessionId }?.createdAt ?: Long.MIN_VALUE
        }

    /** The quest is not recorded in any session - a normal state, and a common one. */
    val hasNoSessions: Boolean
        get() = links.isEmpty() && !isLoading

    /** The campaign has no sessions at all, so there is nothing to record the quest in. */
    val hasNoCandidates: Boolean
        get() = candidates.isEmpty() && !isLoading

    /** The session the picker currently names, or `null` while nothing is picked. */
    val selectedSession: Session?
        get() = candidates.firstOrNull { it.id == selectedSessionId }
}
