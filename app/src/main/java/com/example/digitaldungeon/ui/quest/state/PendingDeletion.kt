package com.example.digitaldungeon.ui.quest.state

import com.example.digitaldungeon.ui.text.UiText

/**
 * A quest the user asked to delete, kept in state until the confirmation dialog is answered.
 *
 * The title travels with the request for the same reason it does for a campaign or a session: the
 * dialog has to name what is about to disappear, and it is shown over a list where the row that
 * was tapped is no longer identifiable.
 */
data class PendingQuestDeletion(
    val questId: Int,
    val questTitle: String
)

/**
 * An objective the user asked to delete, kept in state until the confirmation dialog is answered.
 *
 * A separate type from [PendingQuestDeletion] on purpose, the same way the campaign package keeps
 * its two apart: the two deletions destroy different things and are confirmed with different
 * wording, so they must not be collapsed into one nullable id.
 */
data class PendingObjectiveDeletion(
    val objectiveId: Int,
    val objectiveTitle: String
)

/** A reward the user asked to delete, kept in state until the confirmation is answered. */
data class PendingRewardDeletion(
    val rewardId: Int,
    val rewardTitle: String
)

/** A consequence the user asked to delete, kept in state until the confirmation is answered. */
data class PendingConsequenceDeletion(
    val consequenceId: Int,
    val consequenceTitle: String
)

/**
 * A note the user asked to delete.
 *
 * Carries only the id, unlike every other pending deletion here: a note has no title to name it
 * by, so the dialog asks about "this note" and the text of the note stays where the user can still
 * read it behind the dialog.
 */
data class PendingNoteDeletion(
    val noteId: Int
)

/**
 * A link the user asked to remove, kept in state until the confirmation is answered.
 *
 * Carries the title of the quest at the *other* end, because that is what the question has to name:
 * "remove the link to Strahd's invitation" says what disappears, while the row's own id says
 * nothing. Neither quest is deleted, and the dialog says that too.
 *
 * A [UiText] and not a `String`, because the title is not always known: the links and the
 * campaign's quests arrive on two flows, so a link can name an id whose title has not landed. The
 * fallback is then a localized sentence, which only the screen can resolve.
 */
data class PendingRelationDeletion(
    val relationId: Int,
    val otherQuestTitle: UiText
)

/**
 * A record that a quest came up in a session, which the user asked to remove.
 *
 * Carries the session's title as [UiText] for the same reason the link deletion does: the title may
 * not have landed yet, and the fallback sentence is only resolvable where strings are drawn.
 */
data class PendingSessionLinkDeletion(
    val linkId: Int,
    val sessionTitle: UiText
)
