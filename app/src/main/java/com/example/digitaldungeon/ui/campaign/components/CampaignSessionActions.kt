package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Immutable
import com.example.digitaldungeon.domain.model.Session

/**
 * Everything the session block of the campaign detail screen can ask for -
 * the "add session" form, the per-row actions and the two dialogs - bundled for
 * the same reason as [CampaignBackupActions]: one feature, one wiring, and
 * no-op defaults so a preview or a draw-only test need not repeat eleven
 * lambdas.
 *
 * Opening a session is *not* in here. It is navigation, it is the only callback
 * that leaves the screen, and the screen receives it from the navigation graph
 * rather than from the ViewModel.
 *
 * @param onTitleChange the "new session" title field was typed into.
 * @param onNotesChange the "new session" notes field was typed into.
 * @param onAddSession "Add session" - create one from the form.
 * @param onEditSessionRequest this session should be opened in the edit dialog.
 * @param onEditTitleChange the title field *inside the dialog* was typed into.
 * @param onEditNotesChange the notes field inside the dialog was typed into.
 * @param onConfirmEditSession the dialog's confirm button. May be refused, in
 * which case the dialog stays open carrying
 * [com.example.digitaldungeon.ui.campaign.state.CampaignSessionsUiState.editError].
 * @param onCancelEditSession the edit dialog was dismissed.
 * @param onDeleteSessionRequest this session should be confirmed for deletion -
 * it takes every event recorded in it down with it.
 * @param onConfirmDeleteSession the deletion was confirmed.
 * @param onCancelDeleteSession the confirmation was declined.
 */
@Immutable
data class CampaignSessionActions(
    val onTitleChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onAddSession: () -> Unit = {},
    val onEditSessionRequest: (Session) -> Unit = {},
    val onEditTitleChange: (String) -> Unit = {},
    val onEditNotesChange: (String) -> Unit = {},
    val onConfirmEditSession: () -> Unit = {},
    val onCancelEditSession: () -> Unit = {},
    val onDeleteSessionRequest: (Session) -> Unit = {},
    val onConfirmDeleteSession: () -> Unit = {},
    val onCancelDeleteSession: () -> Unit = {}
)
