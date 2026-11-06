package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Immutable

/**
 * Everything the participant block of the campaign detail screen can ask for,
 * bundled so the stateless screen keeps a parameter list a human can read.
 *
 * Grouped for the same reason as [CampaignBackupActions]: these nine callbacks
 * are one feature, they are always wired to the same ViewModel at once, and the
 * no-op defaults are what lets a preview - or a test that only looks at what is
 * drawn - show the block without repeating nine lambdas it does not care about.
 *
 * The collection is edited in place and saved as a whole, which is why the row
 * callbacks take a position rather than a participant: two party members may
 * legitimately share a name, and only the index says which row was tapped.
 *
 * @param onNameChange the name field was typed into.
 * @param onLevelChange the level field was typed into.
 * @param onMaxHpChange the max HP field was typed into.
 * @param onAdd "Add participant" - append what the form currently holds.
 * @param onUpdate "Update participant" - write the form back to the row being
 * edited. Only reachable while
 * [com.example.digitaldungeon.ui.campaign.state.CampaignParticipantsUiState.isEditing].
 * @param onCancelEdit the edit was abandoned; the form returns to adding.
 * @param onEditParticipant row at this index should be loaded into the form.
 * @param onRemoveParticipant row at this index should be dropped from the
 * editable collection. Nothing is persisted until [onSave].
 * @param onSave "Save changes" - persist the collection as it stands.
 */
@Immutable
data class CampaignParticipantActions(
    val onNameChange: (String) -> Unit = {},
    val onLevelChange: (String) -> Unit = {},
    val onMaxHpChange: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onUpdate: () -> Unit = {},
    val onCancelEdit: () -> Unit = {},
    val onEditParticipant: (Int) -> Unit = {},
    val onRemoveParticipant: (Int) -> Unit = {},
    val onSave: () -> Unit = {}
)
