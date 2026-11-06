package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.ui.components.CreatedAtLabel
import com.example.digitaldungeon.ui.components.PREVIEW_CREATED_AT
import com.example.digitaldungeon.ui.session.components.SessionCard
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme

/**
 * One row of the session list on the campaign detail screen: the session card
 * plus the actions the campaign screen adds on top of it.
 *
 * "Edit" and the creation date sit next to the card rather than inside it because
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Out here for the same reason "Edit" is: SessionCard belongs to the
            // session UI, so the campaign side adds what it needs around it.
            // "Created" and not "Played" on purpose - the field records when the
            // session was written down, which is usually but not provably the
            // night it was played, and a journal must not claim more than it knows.
            CreatedAtLabel(
                createdAt = session.createdAt,
                modifier = Modifier.padding(start = 4.dp)
            )

            TextButton(
                onClick = onEdit
            ) {
                Text(stringResource(R.string.campaign_session_edit_action))
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
                createdAt = PREVIEW_CREATED_AT
            ),
            onOpen = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
