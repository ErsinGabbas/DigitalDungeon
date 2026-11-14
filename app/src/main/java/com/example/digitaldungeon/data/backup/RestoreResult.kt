package com.example.digitaldungeon.data.backup

/**
 * Outcome of [BackupService.restore].
 *
 * A restore is driven by a file the user picked by hand, so picking the wrong one is normal input,
 * not an exceptional condition: every foreseeable failure is one of these values and nothing is
 * thrown.
 */
sealed interface RestoreResult {

    /**
     * The database now holds exactly the contents of the document. Counts are what was written,
     * so a caller can tell the user how much came back.
     */
    data class Success(
        val campaigns: Int,
        val campaignParticipants: Int,
        val sessions: Int,
        val sessionEvents: Int,
        /**
         * Quests written back. Defaults to zero so that restoring a format version 1 document -
         * one written before the quest tracker existed - is spelled out as "no quests came back"
         * rather than left unsaid.
         */
        val quests: Int = 0,
        /** Quest objectives written back; zero for a document older than format version 3. */
        val questObjectives: Int = 0,
        /** Quest rewards written back; zero for a document older than format version 4. */
        val questRewards: Int = 0,
        /** Quest consequences written back; zero for a document older than format version 4. */
        val questConsequences: Int = 0,
        /** Quest notes written back; zero for a document older than format version 4. */
        val questNotes: Int = 0,
        /** Links between quests written back; zero for a document older than format version 5. */
        val questRelations: Int = 0,
        /** History entries written back; zero for a document older than format version 6. */
        val questHistoryEntries: Int = 0,
        /** Quest-to-session links written back; zero for a document older than format version 6. */
        val questSessionLinks: Int = 0
    ) : RestoreResult {

        /**
         * Everything that was written, down to the parts of a quest.
         *
         * Note that the banner the user reads does not list every one of these counts - see
         * `BackupMessage.RestoreSucceeded`. This number is the honest total, so it is larger than
         * the sum of the counts shown there.
         */
        val totalRecords: Int
            get() = campaigns + campaignParticipants + sessions + sessionEvents +
                quests + questObjectives + questRewards + questConsequences + questNotes +
                questRelations + questHistoryEntries + questSessionLinks
    }

    /**
     * The text is not a backup document: it does not parse as JSON, lacks the version field, or
     * parses but is not a coherent snapshot (ids that cannot be restored verbatim). The database
     * was not touched.
     */
    data object InvalidJson : RestoreResult

    /**
     * A backup document of a format this build does not know, typically written by a newer
     * version of the app. The database was not touched.
     *
     * @param supportedFormatVersion the newest format this build reads; older ones are read too,
     *   see [SUPPORTED_BACKUP_FORMAT_VERSIONS].
     */
    data class UnsupportedFormatVersion(
        val formatVersion: Int,
        val supportedFormatVersion: Int = BACKUP_FORMAT_VERSION
    ) : RestoreResult
}
