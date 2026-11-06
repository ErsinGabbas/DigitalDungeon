package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.components.ConfirmDeleteDialog
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The wall in front of a restore.
 *
 * Built on [ConfirmDeleteDialog] rather than on a second dialog of its own,
 * because that is exactly what this is: a restore is a delete of everything with
 * a load afterwards, and it deserves the same red confirm button as deleting a
 * campaign - the destructive half is the part that cannot be taken back.
 *
 * The wording spells out what "restore" hides. "Restore" sounds additive and is
 * not: a restore replaces the database, so every campaign, session and event that
 * is not in the chosen file is gone, including games recorded after the backup was
 * taken. The confirm label repeats it, so that the last thing read before the tap
 * is "Replace my data" and not "OK".
 *
 * @param onConfirm the user accepted the warning. Expected to open the file
 * picker, not to start the restore - nothing has been chosen yet at this point.
 */
@Composable
fun ConfirmRestoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {

    ConfirmDeleteDialog(
        title = stringResource(R.string.backup_restore_warning_title),
        text = stringResource(R.string.backup_restore_warning_text),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        confirmLabel = stringResource(R.string.backup_restore_warning_confirm),
        dismissLabel = stringResource(R.string.common_cancel)
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmRestoreDialogPreview() {
    DigitalDungeonTheme {
        ConfirmRestoreDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
