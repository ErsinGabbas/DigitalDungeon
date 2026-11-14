package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.campaign.state.BackupMessage
import com.example.digitaldungeon.ui.text.asString

/**
 * The sentence a [BackupMessage] is read as, in the language the screen is being
 * drawn in.
 *
 * The counted outcomes are resolved here and not in the ViewModel because the
 * plural form a number takes is a property of the language: English picks
 * between two forms, Russian between four, and a count formatted into "%d
 * campaigns" by hand is wrong in most of them. `pluralStringResource` asks the
 * resource system which form the number needs, which is the only place that
 * knows.
 */
@Composable
fun BackupMessage.asBannerText(): String = when (this) {

    is BackupMessage.Text -> text.asString()

    is BackupMessage.ExportSucceeded -> pluralStringResource(
        R.plurals.backup_export_success,
        campaignCount,
        campaignCount
    )

    // Six counts, each pluralised on its own and placed by the frame, so a
    // translation can put the whole list in whatever order it reads best in.
    is BackupMessage.RestoreSucceeded -> stringResource(
        R.string.backup_restore_success,
        pluralStringResource(R.plurals.backup_record_count, totalRecords, totalRecords),
        pluralStringResource(R.plurals.backup_campaign_count, campaigns, campaigns),
        pluralStringResource(
            R.plurals.backup_participant_count,
            participants,
            participants
        ),
        pluralStringResource(R.plurals.backup_quest_count, quests, quests),
        pluralStringResource(R.plurals.backup_session_count, sessions, sessions),
        pluralStringResource(R.plurals.backup_event_count, events, events)
    )
}
