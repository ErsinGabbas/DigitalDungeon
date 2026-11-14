package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestRelation
import com.example.digitaldungeon.domain.model.QuestRelationType
import com.example.digitaldungeon.ui.text.UiText

/**
 * What a link says *from the point of view of the quest being looked at*.
 *
 * A presentation concept, not a stored one. The table holds four types and a direction; this is the
 * six sentences a DM actually picks between, and the difference is the whole reason it exists:
 * `PARENT_CHILD` means "leads to" when read from the parent and "is part of" when read from the
 * child, and the same row is both at once depending on which quest is on screen.
 *
 * The mapping to and from the stored row lives in the ViewModel, which is the only place that knows
 * which quest "this quest" is.
 */
enum class QuestLinkKind {

    /** This quest is the parent: the other quest hangs under it. */
    PARENT_OF,

    /** This quest hangs under the other one. */
    CHILD_OF,

    /** This quest comes first. */
    PRECEDES,

    /** The other quest comes first. */
    FOLLOWS,

    /** Either instead of the other, not both. Symmetric. */
    ALTERNATIVE,

    /** Connected, without a direction or a hierarchy. Symmetric. */
    RELATED
}

/**
 * One link as the screen draws it: the row that was stored, read from one end, with the other end
 * named.
 *
 * @param otherQuestTitle `null` when the quest at the far end is not among the loaded ones. That is
 *   a real state and not a bug - the links and the campaign's quests arrive on two separate flows,
 *   so for one frame a link can know an id whose title has not landed yet.
 */
data class QuestLink(
    val relation: QuestRelation,
    val kind: QuestLinkKind,
    val otherQuestId: Int,
    val otherQuestTitle: String?
)

/**
 * State of the links block on the quest screen.
 *
 * @param questId the quest being looked at. Every link is read from this end, which is what decides
 *   whether a parent row reads as "leads to" or as "is part of".
 * @param candidates the other quests of the same campaign, i.e. what the picker offers. Scoped to
 *   the campaign because that is the rule the repository enforces on the way in; offering a quest
 *   it would refuse would be offering a dead end.
 * @param formError why the last attempt was refused - the picker was empty, or the repository said
 *   no. Separate from [error], which is a write that failed outright.
 */
data class QuestRelationsUiState(
    val questId: Int = 0,
    val relations: List<QuestRelation> = emptyList(),
    val candidates: List<Quest> = emptyList(),
    val isLoading: Boolean = false,
    /** Formatted when drawn, so it follows a language change. See [UiText]. */
    val error: UiText? = null,
    val selectedTargetId: Int? = null,
    val selectedKind: QuestLinkKind = QuestLinkKind.RELATED,
    val formError: UiText? = null,
    val pendingDeletion: PendingRelationDeletion? = null
) {

    /**
     * Every link, read from this quest's end and paired with the title of the far end.
     *
     * Derived rather than stored, like every other view in this app: the stored rows say nothing
     * about which end is "here", and recomputing that is cheaper than keeping a second list in step
     * with two flows.
     *
     * Ordered by [QuestLinkKind] so the groups below always come out in the same sequence - what a
     * quest is part of first, then what it leads to, then the order it sits in, then the
     * alternatives. A list of links that reshuffled itself as rows were added would be unreadable.
     */
    val links: List<QuestLink> = relations
        .mapNotNull { relation -> relation.toLink(questId, candidates) }
        .sortedBy { it.kind.ordinal }

    /** The quest is in no links at all - a normal, complete state. */
    val hasNoLinks: Boolean
        get() = links.isEmpty() && !isLoading

    /**
     * There is nothing to link to: the campaign holds this quest and nothing else.
     *
     * Worth its own sentence, because an empty picker with an enabled button would look broken.
     */
    val hasNoCandidates: Boolean
        get() = candidates.isEmpty() && !isLoading

    /** The quest the picker currently names, or `null` while nothing is picked. */
    val selectedTarget: Quest?
        get() = candidates.firstOrNull { it.id == selectedTargetId }
}

/**
 * The same row, read from [questId].
 *
 * `null` when the row does not actually touch this quest, which the query makes impossible but
 * which a derived view should not depend on being impossible.
 */
private fun QuestRelation.toLink(
    questId: Int,
    candidates: List<Quest>
): QuestLink? {

    val isSource = sourceQuestId == questId
    val isTarget = targetQuestId == questId

    if (!isSource && !isTarget) return null

    val otherId = if (isSource) targetQuestId else sourceQuestId

    val kind = when (relationType) {
        // Direction decides which sentence this row makes here.
        QuestRelationType.PARENT_CHILD ->
            if (isSource) QuestLinkKind.PARENT_OF else QuestLinkKind.CHILD_OF

        QuestRelationType.PRECEDES ->
            if (isSource) QuestLinkKind.PRECEDES else QuestLinkKind.FOLLOWS

        QuestRelationType.ALTERNATIVE -> QuestLinkKind.ALTERNATIVE
        QuestRelationType.RELATED -> QuestLinkKind.RELATED

        // Never stored - the repository rewrites it as PRECEDES - but the enum still has it, so
        // the branch has to say what it would mean rather than pretend it cannot happen.
        QuestRelationType.FOLLOWS ->
            if (isSource) QuestLinkKind.FOLLOWS else QuestLinkKind.PRECEDES
    }

    return QuestLink(
        relation = this,
        kind = kind,
        otherQuestId = otherId,
        otherQuestTitle = candidates.firstOrNull { it.id == otherId }?.title
    )
}
