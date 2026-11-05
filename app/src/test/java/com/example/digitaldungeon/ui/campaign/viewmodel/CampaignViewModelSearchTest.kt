package com.example.digitaldungeon.ui.campaign.viewmodel

import com.example.digitaldungeon.testing.FakeCampaignRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.campaign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Searching the campaign list.
 *
 * The filtered list is derived, never stored, so every test here also asserts what
 * happened to the *unfiltered* `campaigns` - that is what keeps "there is nothing
 * yet" and "the search hid everything" apart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CampaignViewModelSearchTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeCampaignRepository

    @Before
    fun setUp() {
        repository = FakeCampaignRepository()
    }

    private fun TestScope.createViewModel(): CampaignViewModel =
        CampaignViewModel(repository).also { advanceUntilIdle() }

    private val stored = listOf(
        campaign(id = 1, name = "Curse of Strahd", description = "gothic horror in Barovia"),
        campaign(id = 2, name = "Tomb of Annihilation", description = "jungle and death traps"),
        campaign(id = 3, name = "Dragon Heist", description = "a city caper in Waterdeep")
    )

    // --------------------------------------------------------------- matching

    @Test
    fun blankSearchQuery_showsEveryCampaign() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            assertEquals(stored, viewModel.uiState.value.filteredCampaigns)
            assertFalse(viewModel.uiState.value.isSearchActive)
        }

    @Test
    fun searchQuery_matchesCampaignName() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Strahd")

            assertEquals(
                listOf(1),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
        }

    @Test
    fun searchQuery_matchesCampaignDescription() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            // "jungle" appears in no name at all, only in a description.
            viewModel.onSearchQueryChange("jungle")

            assertEquals(
                listOf(2),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
        }

    @Test
    fun searchQuery_ignoresCase() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("sTRAHD")

            assertEquals(
                listOf(1),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
        }

    @Test
    fun searchQuery_isTrimmedBeforeMatching() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            // Untrimmed, "  Strahd  " matches nothing.
            viewModel.onSearchQueryChange("  Strahd  ")

            assertEquals(
                listOf(1),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
        }

    @Test
    fun whitespaceOnlySearchQuery_showsEveryCampaign() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("   ")

            assertEquals(stored, viewModel.uiState.value.filteredCampaigns)
            assertFalse(viewModel.uiState.value.isSearchActive)
        }

    @Test
    fun searchQuery_canMatchSeveralCampaigns() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            // "of" is in two names; "a city caper" has none.
            viewModel.onSearchQueryChange("of")

            assertEquals(
                listOf(1, 2),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
        }

    @Test
    fun searchQuery_isStoredInState() =
        runTest(mainDispatcherRule.testDispatcher) {

            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Strahd")

            assertEquals("Strahd", viewModel.uiState.value.searchQuery)
            assertTrue(viewModel.uiState.value.isSearchActive)
        }

    // --------------------------------------------- unfiltered list stays intact

    @Test
    fun searchQuery_leavesUnfilteredCampaignsUntouched() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Strahd")

            assertEquals(stored, viewModel.uiState.value.campaigns)
        }

    @Test
    fun campaignsEmission_whileSearching_isStillFiltered() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Strahd")

            repository.campaigns.value = stored +
                campaign(id = 4, name = "Strahd Returns", description = "again")
            advanceUntilIdle()

            assertEquals(
                listOf(1, 4),
                viewModel.uiState.value.filteredCampaigns.map { it.id }
            )
            assertEquals(4, viewModel.uiState.value.campaigns.size)
        }

    // ------------------------------------------------------- clearing the query

    @Test
    fun clearSearchQuery_restoresTheFullList() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Strahd")

            viewModel.clearSearchQuery()

            assertEquals("", viewModel.uiState.value.searchQuery)
            assertEquals(stored, viewModel.uiState.value.filteredCampaigns)
            assertFalse(viewModel.uiState.value.isSearchActive)
        }

    @Test
    fun onSearchQueryChange_toEmptyString_restoresTheFullList() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Strahd")

            viewModel.onSearchQueryChange("")

            assertEquals(stored, viewModel.uiState.value.filteredCampaigns)
        }

    // ----------------------------------------------- the two empty states differ

    @Test
    fun withoutAnyCampaign_reportsHasNoCampaigns() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = emptyList()
            val viewModel = createViewModel()

            val state = viewModel.uiState.value
            assertTrue(state.hasNoCampaigns)
            assertFalse(state.hasNoSearchResults)
        }

    @Test
    fun searchMatchingNothing_reportsHasNoSearchResultsNotHasNoCampaigns() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Ravenloft")

            val state = viewModel.uiState.value
            assertTrue(state.filteredCampaigns.isEmpty())
            assertTrue(state.hasNoSearchResults)
            // The campaigns are still there, the search just hid them.
            assertFalse(state.hasNoCampaigns)
        }

    @Test
    fun searchMatchingNothing_withoutAnyCampaign_reportsOnlyHasNoCampaigns() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = emptyList()
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Ravenloft")

            val state = viewModel.uiState.value
            assertTrue(state.hasNoCampaigns)
            assertFalse(state.hasNoSearchResults)
        }

    @Test
    fun searchWithMatches_reportsNeitherEmptyState() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("Strahd")

            val state = viewModel.uiState.value
            assertFalse(state.hasNoCampaigns)
            assertFalse(state.hasNoSearchResults)
        }

    @Test
    fun clearSearchQuery_afterAnEmptyResult_clearsHasNoSearchResults() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.campaigns.value = stored
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Ravenloft")
            assertTrue(viewModel.uiState.value.hasNoSearchResults)

            viewModel.clearSearchQuery()

            assertFalse(viewModel.uiState.value.hasNoSearchResults)
        }
}
