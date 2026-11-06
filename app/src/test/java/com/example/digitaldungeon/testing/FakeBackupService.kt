package com.example.digitaldungeon.testing

import com.example.digitaldungeon.data.backup.BackupService
import com.example.digitaldungeon.data.backup.RestoreResult

/**
 * Hand written stand-in for [BackupService]; see [FakeCampaignRepository] for the
 * conventions (recorded calls, injectable failures).
 *
 * [restore] never throws of its own accord in production - a file that is not a backup
 * comes back as a [RestoreResult] - so [restoreResult] is the knob a test normally turns.
 * [restoreError] exists anyway, to cover the ViewModel's own `catch`.
 */
class FakeBackupService : BackupService {

    /** The document [export] hands back. */
    var exportJson: String = """{"formatVersion":1,"createdAt":0,"campaigns":[]}"""

    var exportError: Exception? = null

    var restoreResult: RestoreResult = RestoreResult.Success(
        campaigns = 0,
        campaignParticipants = 0,
        sessions = 0,
        sessionEvents = 0
    )

    var restoreError: Exception? = null

    /**
     * How often [export] was entered. A count rather than a list because the call has no
     * arguments, and it is incremented before a failure is thrown, so a test can tell
     * "never called" apart from "called and refused".
     */
    var exportCallCount: Int = 0
        private set

    /** The documents [restore] was handed, in order. */
    val restoreCalls = mutableListOf<String>()

    override suspend fun export(): String {
        exportCallCount++
        exportError?.let { throw it }
        return exportJson
    }

    override suspend fun restore(json: String): RestoreResult {
        restoreCalls += json
        restoreError?.let { throw it }
        return restoreResult
    }
}
