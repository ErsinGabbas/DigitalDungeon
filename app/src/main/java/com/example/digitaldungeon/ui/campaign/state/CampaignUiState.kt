package com.example.digitaldungeon.ui.campaign.state

import com.example.digitaldungeon.domain.model.Campaign

data class CampaignUiState(
    val campaigns: List<Campaign> = emptyList(),
    val campaignName: String = "",
    val campaignDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * Non-null while the delete confirmation dialog is open. Lives in the state
     * instead of a `remember` so the question survives a configuration change.
     */
    val pendingDeletion: PendingCampaignDeletion? = null,
    /**
     * Non-null while the edit dialog is open - it is the campaign being renamed.
     * In the state for the same reason as [pendingDeletion]: a rotation must not
     * throw away a half typed name.
     */
    val editingCampaign: Campaign? = null,
    val editName: String = "",
    val editDescription: String = "",
    /**
     * Why the edit dialog refused to save. Separate from [error] because the
     * dialog covers the screen: a message behind it would never be read.
     */
    val editError: String? = null,
    /** What the user typed into the search field, "" while not searching. */
    val searchQuery: String = ""
) {

    /**
     * The campaigns the list should show: [campaigns] filtered by [searchQuery],
     * matched against name and description, ignoring case.
     *
     * Derived instead of stored so that the unfiltered [campaigns] - and with it
     * "are there any campaigns at all" - stays intact while a search is running.
     */
    val filteredCampaigns: List<Campaign>
        get() {

            val query = searchQuery.trim()

            if (query.isEmpty()) return campaigns

            return campaigns.filter { campaign ->
                campaign.name.contains(query, ignoreCase = true) ||
                    campaign.description.contains(query, ignoreCase = true)
            }
        }

    val isSearchActive: Boolean
        get() = searchQuery.isNotBlank()

    /** There is nothing to show because nothing has been created yet. */
    val hasNoCampaigns: Boolean
        get() = campaigns.isEmpty() && !isLoading

    /**
     * There are campaigns, the search just hid all of them - a different state
     * from [hasNoCampaigns] and worth a different message.
     */
    val hasNoSearchResults: Boolean
        get() = campaigns.isNotEmpty() && filteredCampaigns.isEmpty()
}
