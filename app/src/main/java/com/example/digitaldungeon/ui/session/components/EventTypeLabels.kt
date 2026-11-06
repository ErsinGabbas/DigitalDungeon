package com.example.digitaldungeon.ui.session.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.digitaldungeon.R
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * The label a user sees for a [SessionEventType].
 *
 * Lives in the UI layer on purpose. The enum constants themselves are storage: they
 * are persisted as text in the events table and appear as literals in the SQL of the
 * database migrations, so renaming or translating them would break existing data.
 * Printing `type.name` instead was the other side of the same coin — it showed the
 * user the storage format ("DISCOVERY"). This mapping is the single place where the
 * two concerns meet, and the `when` is exhaustive, so a new type cannot be added
 * without giving it a label.
 */
@StringRes
fun eventTypeLabelRes(type: SessionEventType): Int =
    when (type) {
        SessionEventType.SCENE -> R.string.event_type_scene
        SessionEventType.COMBAT -> R.string.event_type_combat
        SessionEventType.DIALOGUE -> R.string.event_type_dialogue
        SessionEventType.DISCOVERY -> R.string.event_type_discovery
        SessionEventType.NOTE -> R.string.event_type_note
    }

/** The localized label of this type, for use inside composition. */
@Composable
fun SessionEventType.label(): String = stringResource(eventTypeLabelRes(this))
