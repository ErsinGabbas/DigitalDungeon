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
        val sessionEvents: Int
    ) : RestoreResult {

        val totalRecords: Int
            get() = campaigns + campaignParticipants + sessions + sessionEvents
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
     */
    data class UnsupportedFormatVersion(
        val formatVersion: Int,
        val supportedFormatVersion: Int = BACKUP_FORMAT_VERSION
    ) : RestoreResult
}
