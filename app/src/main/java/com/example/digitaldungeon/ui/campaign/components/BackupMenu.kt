package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * The app-level backup actions, folded into a [TopAppBar] overflow menu.
 *
 * In the overflow and not as two toolbar icons: both are rare, both are easy to
 * confuse with each other as 24dp glyphs, and one of them replaces the entire
 * database - that is not a button anyone should be able to hit while aiming at
 * something else.
 *
 * Whether the menu is open stays here rather than in the UI state: it is the
 * component's own business and decides nothing. The restore warning is the
 * opposite case and lives in the ViewModel, because it is the one thing standing
 * in front of a destructive operation and must not be lost to a rotation.
 *
 * @param enabled false while an export or a restore is running, so a second
 * whole-database operation cannot be started on top of the first.
 */
@Composable
fun BackupMenu(
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {

    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {

        IconButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription =
                    stringResource(R.string.backup_menu_content_description)
            )
        }

        DropdownMenu(
            // `&& enabled` rather than a state write: a menu left open while an
            // operation starts would offer actions that are refused on click.
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {

            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_export_action)) },
                onClick = {
                    expanded = false
                    onExportBackup()
                }
            )

            DropdownMenuItem(
                // The ellipsis is a promise: neither item does anything on its
                // own, both open a file picker first.
                text = { Text(stringResource(R.string.backup_restore_action)) },
                onClick = {
                    expanded = false
                    onRestoreBackup()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BackupMenuPreview() {
    DigitalDungeonTheme {
        BackupMenu(
            onExportBackup = {},
            onRestoreBackup = {}
        )
    }
}

@Preview(name = "Disabled while running", showBackground = true)
@Composable
private fun BackupMenuDisabledPreview() {
    DigitalDungeonTheme {
        BackupMenu(
            onExportBackup = {},
            onRestoreBackup = {},
            enabled = false
        )
    }
}

/**
 * The menu as it looks open, which is what the two items actually have to be
 * judged by. Rendered as a plain column of items because a [DropdownMenu] is a
 * popup and popups do not show up in a preview.
 */
@Preview(name = "Expanded items", showBackground = true)
@Composable
private fun BackupMenuItemsPreview() {
    DigitalDungeonTheme {
        Column {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_export_action)) },
                onClick = {}
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.backup_restore_action)) },
                onClick = {}
            )
        }
    }
}
