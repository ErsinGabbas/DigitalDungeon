package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.text.UiText

/**
 * The backup operation currently running, or `null` while none is.
 *
 * One value rather than two booleans: an export and a restore can never be in
 * flight at the same time, and a single nullable field makes that impossible to
 * express instead of merely unlikely.
 *
 * @param progressLabel what the progress indicator says while this runs - the two
 * operations take different amounts of time and "please wait" explains nothing.
 * A [UiText] and not a [String], so the label is formatted when it is drawn and
 * follows a language change instead of keeping the one it was created in.
 */
enum class BackupOperation(val progressLabel: UiText) {

    Export(UiText.of(R.string.backup_progress_export)),

    Restore(UiText.of(R.string.backup_progress_restore))
}

/**
 * The result of the last backup operation, kept until the user dismisses it.
 *
 * In the UI state and not in a transient event channel on purpose: a restore
 * replaces everything the user owns, so its outcome has to survive a rotation -
 * a count that vanishes because the screen turned is a count nobody can trust.
 *
 * Why this is a sealed type and not a single string: the two successful outcomes
 * are *counts*, and a count cannot be turned into a sentence outside the UI. How
 * many plural forms a number needs is a property of the language - English has
 * two, Russian four - so the number travels as a number and the screen resolves
 * it through a `<plurals>` resource. Everything that is already a sentence
 * travels as [UiText], which is formatted at render time for the same reason.
 */
sealed interface BackupMessage {

    /** Whether this reports a failure; decides how the banner looks. */
    val isError: Boolean

    /**
     * A message that is a sentence: any failure, with the locale and the format
     * arguments carried along and applied when it is drawn.
     */
    data class Text(
        val text: UiText,
        override val isError: Boolean = false
    ) : BackupMessage

    /**
     * The export finished and wrote [campaignCount] campaigns.
     *
     * The count is the whole message, so the plural form it needs is decided by
     * the reader's language, not here.
     */
    data class ExportSucceeded(
        val campaignCount: Int
    ) : BackupMessage {
        override val isError: Boolean get() = false
    }

    /**
     * The restore finished. The counts are kept per table instead of being summed
     * into [totalRecords] alone, because "384 records" does not tell anyone
     * whether their sessions came back.
     *
     * Deliberately only the things a user counts: campaigns, the party, quests,
     * sessions and events. The *parts* of a quest - its objectives, and everything
     * later stages hang off it - are not listed, even though
     * [com.example.digitaldungeon.data.backup.RestoreResult.Success] carries them
     * and [totalRecords] includes them. A sentence with a dozen numbers in it is
     * one nobody reads, and the quest line already answers the question this
     * message exists for; the parts came back with their quest or not at all, and
     * the quest screen is where they are checked. The consequence, stated here so
     * it is not mistaken for a bug: [totalRecords] is larger than the sum of the
     * counts below.
     */
    data class RestoreSucceeded(
        val totalRecords: Int,
        val campaigns: Int,
        val participants: Int,
        val sessions: Int,
        val events: Int,
        /**
         * Quests that came back. Reported like every other count, and for the same
         * reason: a restore that silently said nothing about quests would leave the
         * one question this message exists to answer unanswered.
         */
        val quests: Int = 0
    ) : BackupMessage {
        override val isError: Boolean get() = false
    }

    companion object {

        /** Shorthand for the common case: a failure that is already a sentence. */
        fun error(text: UiText): Text =
            Text(text = text, isError = true)
    }
}
