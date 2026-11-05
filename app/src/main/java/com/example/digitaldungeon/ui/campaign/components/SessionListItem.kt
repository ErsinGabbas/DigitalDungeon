package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.ui.session.components.SessionCard
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One row of the session list on the campaign detail screen: the session card
 * plus the actions the campaign screen adds on top of it.
 *
 * "Edit" sits next to the card rather than inside it because
 * [SessionCard] belongs to the session UI and is shown in places that do not
 * offer editing - this wrapper keeps the extra entry point on the campaign side
 * without changing the card's contract.
 */
@Composable
fun SessionListItem(
    session: Session,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        SessionCard(
            session = session,
            onDelete = onDelete,
            onClick = onOpen
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                onClick = onEdit
            ) {
                Text("Edit session")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionListItemPreview() {
    DigitalDungeonTheme {
        SessionListItem(
            session = Session(
                id = 1,
                campaignId = 1,
                title = "Session 4 - Under the Sunken Gate",
                notes = "The party finally reaches the flooded undercity.",
                createdAt = 0L
            ),
            onOpen = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
