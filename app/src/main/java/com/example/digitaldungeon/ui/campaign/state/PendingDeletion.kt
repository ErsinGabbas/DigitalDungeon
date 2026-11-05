package com.example.digitaldungeon.ui.campaign.state

/**
 * A campaign the user asked to delete, kept in state until the confirmation
 * dialog is answered.
 *
 * Deleting a campaign cascades to its sessions and to the events of those
 * sessions, so the name travels with the request: the dialog has to be able to
 * name what is about to disappear.
 */
data class PendingCampaignDeletion(
    val campaignId: Int,
    val campaignName: String
)

/**
 * A session the user asked to delete, kept in state until the confirmation
 * dialog is answered.
 *
 * A separate type from [PendingCampaignDeletion] on purpose: the two deletions
 * destroy different things and are confirmed with different wording, so they
 * must not be collapsed into one nullable id.
 */
data class PendingSessionDeletion(
    val sessionId: Int,
    val sessionTitle: String
)
