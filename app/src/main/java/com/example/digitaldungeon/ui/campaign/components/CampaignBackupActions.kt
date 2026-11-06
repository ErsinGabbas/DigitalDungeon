package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Immutable

/**
 * The five callbacks the backup flow needs, bundled so the campaign screen's
 * parameter list stays readable.
 *
 * Grouped rather than passed one by one because they are one feature and are
 * always wired together; the defaults are what lets a preview show the screen
 * without repeating five no-ops it does not care about.
 *
 * @param onExportBackup the user picked "Export backup". Expected to open the
 * system save dialog - the document is only built once a file has been chosen.
 * @param onRestoreBackup the user picked "Restore from backup". Expected to open
 * the warning, not the file picker.
 * @param onRestoreWarningConfirmed the warning was accepted. Expected to close it
 * and open the file picker.
 * @param onRestoreWarningDismissed the warning was declined.
 * @param onDismissMessage the result banner was closed.
 */
@Immutable
data class CampaignBackupActions(
    val onExportBackup: () -> Unit = {},
    val onRestoreBackup: () -> Unit = {},
    val onRestoreWarningConfirmed: () -> Unit = {},
    val onRestoreWarningDismissed: () -> Unit = {},
    val onDismissMessage: () -> Unit = {}
)
