package com.example.digitaldungeon.ui.session.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.digitaldungeon.domain.model.Session

@Composable
fun SessionCard(
    session: Session,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

            Text(
                text = session.title
            )

            Text(
                text = session.notes
            )

            Row(
                horizontalArrangement =
                    Arrangement.End,

                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Button(
                    onClick = onDelete
                ) {

                    Text("Delete")

                }
            }
        }
    }
}