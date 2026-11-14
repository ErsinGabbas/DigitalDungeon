package com.example.digitaldungeon.ui.quest.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.testing.FakeQuestRepository
import com.example.digitaldungeon.testing.MainDispatcherRule
import com.example.digitaldungeon.testing.quest
import com.example.digitaldungeon.ui.quest.state.QuestSort
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
 * The search field, the two filters and the sort order.
 *
 * All four are derived views of the one loaded list, so every assertion here is about
 * `visibleQuests` while `quests` stays untouched: the full list is what "are there any quests at
 * all" is answered from, and losing it would make an empty filter result indistinguishable from an
 * empty campaign.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestListViewModelFilterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeQuestRepository

    @Before
    fun setUp() {
        repository = FakeQuestRepository()
    }

    private fun TestScope.createViewModel(): QuestListViewModel =
        QuestListViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(
                mapOf(QuestListViewModel.CAMPAIGN_ID_KEY to 1)
            )
        ).also { advanceUntilIdle() }

    /**
     * Three quests that differ in every dimension the screen filters and sorts by, so one list
     * serves every test below.
     */
    private fun seedQuests() {
        repository.quests.value = listOf(
            quest(
                id = 1,
                title = "Escort Ireena to Vallaki",
                description = "Ismark asks for help",
                category = QuestCategory.MAIN,
                status = QuestStatus.ACTIVE,
                priority = QuestPriority.HIGH,
                createdAt = 3_000L
            ),
            quest(
                id = 2,
                title = "The miller's daughter",
                description = "Rumours in the village",
                category = QuestCategory.SIDE,
                status = QuestStatus.NOT_STARTED,
                priority = QuestPriority.NORMAL,
                createdAt = 2_000L
            ),
            quest(
                id = 3,
                title = "Strahd's invitation",
                description = "Dinner at Ravenloft",
                category = QuestCategory.OTHER,
                status = QuestStatus.COMPLETED,
                priority = QuestPriority.LOW,
                createdAt = 1_000L
            )
        )
    }

    // -------------------------------------------------------------- search

    @Test
    fun searchQuery_matchesTheTitleCaseInsensitively() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("IREENA")

            assertEquals(listOf(1), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun searchQuery_matchesTheDescriptionToo() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            // "the miller's daughter" is as likely to be described as named.
            viewModel.onSearchQueryChange("rumours")

            assertEquals(listOf(2), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun searchQuery_neverShrinksTheFullList() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("nothing matches this")

            val state = viewModel.uiState.value
            assertTrue(state.visibleQuests.isEmpty())
            assertEquals(3, state.quests.size)
            // Which is what lets the screen say "nothing matched" instead of "no quests yet".
            assertTrue(state.hasNoMatches)
            assertFalse(state.hasNoQuests)
        }

    @Test
    fun searchQuery_isTrimmedBeforeMatching() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onSearchQueryChange("  Strahd  ")

            assertEquals(listOf(3), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun onClearSearchQuery_showsEverythingAgain() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Ireena")

            viewModel.onClearSearchQuery()

            assertEquals(3, viewModel.uiState.value.visibleQuests.size)
            assertFalse(viewModel.uiState.value.isFilterActive)
        }

    // ------------------------------------------------------------- filters

    @Test
    fun statusFilter_keepsOnlyThatStatus() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onStatusFilterSelected(QuestStatus.COMPLETED)

            assertEquals(listOf(3), viewModel.uiState.value.visibleQuests.map { it.id })
            assertTrue(viewModel.uiState.value.isFilterActive)
        }

    @Test
    fun statusFilter_setToNull_showsEveryStatus() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()
            viewModel.onStatusFilterSelected(QuestStatus.COMPLETED)

            viewModel.onStatusFilterSelected(null)

            assertEquals(3, viewModel.uiState.value.visibleQuests.size)
            assertFalse(viewModel.uiState.value.isFilterActive)
        }

    @Test
    fun categoryFilter_keepsOnlyThatCategory() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onCategoryFilterSelected(QuestCategory.MAIN)

            assertEquals(listOf(1), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun filtersAndSearch_applyTogether() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onStatusFilterSelected(QuestStatus.ACTIVE)
            viewModel.onCategoryFilterSelected(QuestCategory.MAIN)
            viewModel.onSearchQueryChange("Vallaki")

            assertEquals(listOf(1), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun contradictoryFilters_hideEverythingWithoutLosingTheList() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            // Quest 1 is the only ACTIVE one, and it is not a SIDE quest.
            viewModel.onStatusFilterSelected(QuestStatus.ACTIVE)
            viewModel.onCategoryFilterSelected(QuestCategory.SIDE)

            val state = viewModel.uiState.value
            assertTrue(state.visibleQuests.isEmpty())
            assertEquals(3, state.quests.size)
        }

    @Test
    fun onClearFilters_resetsSearchAndBothFiltersButKeepsTheSortOrder() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()
            viewModel.onSearchQueryChange("Ireena")
            viewModel.onStatusFilterSelected(QuestStatus.ACTIVE)
            viewModel.onCategoryFilterSelected(QuestCategory.MAIN)
            viewModel.onSortSelected(QuestSort.NEWEST)

            viewModel.onClearFilters()

            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertEquals(null, state.statusFilter)
            assertEquals(null, state.categoryFilter)
            assertFalse(state.isFilterActive)
            // The order hides nothing, so it is not one of the things "reset filters" is about.
            assertEquals(QuestSort.NEWEST, state.sort)
        }

    // ---------------------------------------------------------------- sort

    @Test
    fun defaultSort_isByPriority() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            assertEquals(QuestSort.PRIORITY, viewModel.uiState.value.sort)
            // HIGH, NORMAL, LOW - the declaration order of the enum.
            assertEquals(listOf(1, 2, 3), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun prioritySort_putsHighFirstRegardlessOfTheOrderTheyArrivedIn() =
        runTest(mainDispatcherRule.testDispatcher) {

            // Deliberately reversed on arrival: an alphabetical `ORDER BY priority` would read
            // HIGH, LOW, NORMAL, which is why the ordering is not left to the query.
            repository.quests.value = listOf(
                quest(id = 1, priority = QuestPriority.LOW, createdAt = 3_000L),
                quest(id = 2, priority = QuestPriority.NORMAL, createdAt = 2_000L),
                quest(id = 3, priority = QuestPriority.HIGH, createdAt = 1_000L)
            )
            val viewModel = createViewModel()

            assertEquals(listOf(3, 2, 1), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun prioritySort_breaksTiesWithTheNewestFirst() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.quests.value = listOf(
                quest(id = 1, priority = QuestPriority.NORMAL, createdAt = 1_000L),
                quest(id = 2, priority = QuestPriority.NORMAL, createdAt = 3_000L),
                quest(id = 3, priority = QuestPriority.NORMAL, createdAt = 2_000L)
            )
            val viewModel = createViewModel()

            assertEquals(listOf(2, 3, 1), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun newestSort_ignoresPriorityEntirely() =
        runTest(mainDispatcherRule.testDispatcher) {

            seedQuests()
            val viewModel = createViewModel()

            viewModel.onSortSelected(QuestSort.NEWEST)

            assertEquals(listOf(1, 2, 3), viewModel.uiState.value.visibleQuests.map { it.id })
        }

    @Test
    fun sortAppliesToTheFilteredListAndNotToTheWholeOne() =
        runTest(mainDispatcherRule.testDispatcher) {

            repository.quests.value = listOf(
                quest(id = 1, status = QuestStatus.ACTIVE, priority = QuestPriority.LOW),
                quest(id = 2, status = QuestStatus.PAUSED, priority = QuestPriority.HIGH),
                quest(id = 3, status = QuestStatus.ACTIVE, priority = QuestPriority.HIGH)
            )
            val viewModel = createViewModel()

            viewModel.onStatusFilterSelected(QuestStatus.ACTIVE)

            assertEquals(listOf(3, 1), viewModel.uiState.value.visibleQuests.map { it.id })
        }
}
