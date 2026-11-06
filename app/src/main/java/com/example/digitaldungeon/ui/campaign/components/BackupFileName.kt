package com.example.digitaldungeon.ui.campaign.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Prefix every backup file gets, so a file picker full of downloads stays readable. */
private const val BACKUP_FILE_PREFIX = "digitaldungeon-backup-"

private const val BACKUP_FILE_EXTENSION = ".json"

/**
 * Name offered to the system save dialog, e.g. `digitaldungeon-backup-2026-11-06.json`.
 *
 * `yyyy-MM-dd` in [Locale.US] on purpose, and the one place in this app where the
 * date is *not* localised: a file name is not prose. ISO order sorts
 * chronologically in any file manager, and the fixed locale keeps the digits
 * Western and the separators legal - a locale-formatted date could produce
 * slashes, which no file name may contain.
 *
 * @param nowMillis when the backup is being taken; defaults to now.
 */
fun backupFileName(nowMillis: Long = System.currentTimeMillis()): String {

    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .format(Date(nowMillis))

    return "$BACKUP_FILE_PREFIX$date$BACKUP_FILE_EXTENSION"
}
