package com.example.digitaldungeon.ui.campaign.components

import androidx.compose.runtime.Immutable
import com.example.digitaldungeon.domain.model.Campaign

/**
 * Everything the campaign list can ask of its ViewModel - the "add campaign"
 * form, the search field, the per-card actions and the two dialogs - bundled for
 * the same reason as [CampaignBackupActions]: they are wired together as one, and
 * the no-op defaults let a preview or a draw-only test render the screen without
 * repeating thirteen lambdas it has no interest in.
 *
 * Opening a campaign is *not* in here. It is navigation, it is the one callback
 * that leaves the screen, and it comes from the navigation graph rather than from
 * the ViewModel - so it stays a parameter of its own and has no default.
 *
 * @param onNameChange the "new campaign" name field was typed into.
 * @param onDescriptionChange the "new campaign" description field was typed into.
 * @param onAddCampaign "Add campaign" - create one from the form.
 * @param onSearchQueryChange the search field was typed into.
 * @param onClearSearchQuery the search field's clear button.
 * @param onEditCampaignRequest this campaign should be opened in the edit dialog.
 * @param onEditNameChange the name field *inside the dialog* was typed into.
 * @param onEditDescriptionChange the description field inside the dialog.
 * @param onConfirmEditCampaign the dialog's confirm button. May be refused, in
 * which case the dialog stays open carrying
 * [com.example.digitaldungeon.ui.campaign.state.CampaignUiState.editError].
 * @param onCancelEditCampaign the edit dialog was dismissed.
 * @param onDeleteCampaignRequest this campaign should be confirmed for deletion -
 * it cascades through every session and event it owns.
 * @param onConfirmDeleteCampaign the deletion was confirmed.
 * @param onCancelDeleteCampaign the confirmation was declined.
 */
@Immutable
data class CampaignListActions(
    val onNameChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onAddCampaign: () -> Unit = {},
    val onSearchQueryChange: (String) -> Unit = {},
    val onClearSearchQuery: () -> Unit = {},
    val onEditCampaignRequest: (Campaign) -> Unit = {},
    val onEditNameChange: (String) -> Unit = {},
    val onEditDescriptionChange: (String) -> Unit = {},
    val onConfirmEditCampaign: () -> Unit = {},
    val onCancelEditCampaign: () -> Unit = {},
    val onDeleteCampaignRequest: (Campaign) -> Unit = {},
    val onConfirmDeleteCampaign: () -> Unit = {},
    val onCancelDeleteCampaign: () -> Unit = {}
)
