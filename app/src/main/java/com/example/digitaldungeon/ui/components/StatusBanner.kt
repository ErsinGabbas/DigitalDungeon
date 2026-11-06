package com.example.digitaldungeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * Outcome of a one-off operation, shown in place until the user dismisses it.
 *
 * A snackbar was the obvious alternative and is what the session screen uses for
 * its errors, but it is the wrong tool here: the result of a restore is a count
 * the user wants to read and possibly re-read ("did all 300 events come back?"),
 * and a snackbar takes it away after a few seconds whether it was read or not.
 * This stays, is coloured, and has to be closed by hand - it cannot be missed.
 *
 * @param isError picks the colours and the icon: a failed restore must not look
 * like a successful one at a glance.
 */
@Composable
fun StatusBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    dismissContentDescription: String = stringResource(R.string.common_dismiss_message)
) {

    val containerColor =
        if (isError) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.tertiaryContainer

    val contentColor =
        if (isError) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    if (isError) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = dismissContentDescription
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBannerPreview() {
    DigitalDungeonTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            StatusBanner(
                text = pluralStringResource(R.plurals.backup_export_success, 3, 3),
                onDismiss = {}
            )

            StatusBanner(
                text = stringResource(R.string.backup_restore_not_a_backup),
                onDismiss = {},
                isError = true
            )
        }
    }
}
