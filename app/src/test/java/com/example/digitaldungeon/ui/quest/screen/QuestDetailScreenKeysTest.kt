package com.example.digitaldungeon.ui.quest.screen

import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestRelation
import com.example.digitaldungeon.domain.model.QuestRelationType
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.domain.model.QuestSessionLink
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.ui.quest.state.QuestConsequencesUiState
import com.example.digitaldungeon.ui.quest.state.QuestDetailUiState
import com.example.digitaldungeon.ui.quest.state.QuestHistoryUiState
import com.example.digitaldungeon.ui.quest.state.QuestNotesUiState
import com.example.digitaldungeon.ui.quest.state.QuestObjectivesUiState
import com.example.digitaldungeon.ui.quest.state.QuestRelationsUiState
import com.example.digitaldungeon.ui.quest.state.QuestRewardsUiState
import com.example.digitaldungeon.ui.quest.state.QuestSessionsUiState
import com.example.digitaldungeon.ui.theme.DigitalDungeonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The quest screen draws eight sections into one `LazyColumn`, and this is the test that keeps their
 * item keys from colliding.
 *
 * It exists because they did collide. Every section used the row's own database id as its key, and
 * those ids are per-table: objective 1, reward 1, consequence 1, note 1, relation 1, session link 1
 * and history entry 1 all produced the key `1`. Compose rejects a duplicate key with
 * `IllegalArgumentException`, so the screen crashed as soon as a quest had a row in more than one
 * section - which is to say, as soon as anyone used the feature. Nothing caught it: the repository
 * tests do not render, and the screen had no UI test of its own.
 *
 * So the fixture is deliberately the worst case rather than a realistic one: **id 1 in all seven
 * lists**. A fixture with distinct ids would pass against the broken code and prove nothing.
 *
 * Scrolling matters as much as rendering. `LazyColumn` only composes what is on screen, so the
 * duplicate key is not reached until the later sections are measured; the original crash appeared on
 * a scroll, not on open.
 */
@RunWith(RobolectricTestRunner::class)
class QuestDetailScreenKeysTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val quest = Quest(
        id = 1,
        campaignId = 1,
        title = "The miller's missing daughter",
        description = "Ask at the mill",
        category = QuestCategory.SIDE,
        status = QuestStatus.ACTIVE,
        priority = QuestPriority.NORMAL,
        isHidden = false,
        createdAt = 1_000L
    )

    /** The second quest exists only so the relation has a far end with a title. */
    private val otherQuest = quest.copy(id = 2, title = "Escort Ireena to Vallaki")

    private fun showEverySection() {
        composeTestRule.setContent {
            DigitalDungeonTheme {
                QuestDetailScreenContent(
                    state = QuestDetailUiState(questId = quest.id, quest = quest),
                    // Every list below holds exactly one row, and every row has id 1.
                    objectivesState = QuestObjectivesUiState(
                        objectives = listOf(
                            QuestObjective(
                                id = 1,
                                questId = 1,
                                title = "Search the mill",
                                description = "",
                                status = QuestObjectiveStatus.NOT_STARTED,
                                orderIndex = 0,
                                isOptional = false
                            )
                        )
                    ),
                    rewardsState = QuestRewardsUiState(
                        rewards = listOf(
                            QuestReward(
                                id = 1,
                                questId = 1,
                                title = "50 gp",
                                description = "",
                                type = QuestRewardType.MONEY
                            )
                        )
                    ),
                    consequencesState = QuestConsequencesUiState(
                        consequences = listOf(
                            QuestConsequence(
                                id = 1,
                                questId = 1,
                                title = "The miller owes a favour",
                                description = "",
                                type = QuestConsequenceType.NPC
                            )
                        )
                    ),
                    notesState = QuestNotesUiState(
                        notes = listOf(
                            QuestNote(
                                id = 1,
                                questId = 1,
                                content = "She is already dead",
                                createdAt = 1_000L,
                                updatedAt = 1_000L
                            )
                        )
                    ),
                    relationsState = QuestRelationsUiState(
                        questId = quest.id,
                        relations = listOf(
                            QuestRelation(
                                id = 1,
                                sourceQuestId = 1,
                                targetQuestId = 2,
                                relationType = QuestRelationType.PARENT_CHILD
                            )
                        ),
                        candidates = listOf(quest, otherQuest)
                    ),
                    historyState = QuestHistoryUiState(
                        entries = listOf(
                            QuestHistoryEntry(
                                id = 1,
                                questId = 1,
                                eventType = QuestHistoryEventType.QUEST_STATUS_CHANGED,
                                description = QuestStatus.ACTIVE.name,
                                timestamp = 2_000L
                            )
                        )
                    ),
                    sessionsState = QuestSessionsUiState(
                        links = listOf(QuestSessionLink(id = 1, questId = 1, sessionId = 1))
                    ),
                    onBack = {},
                    onEditQuest = {}
                )
            }
        }
    }

    /**
     * The screen's one scrollable, which is the `LazyColumn` every section writes into.
     *
     * Addressed by its scroll action rather than by a test tag: the tag would exist only for this
     * test, and what is being tested is the real list.
     */
    private fun lazyColumn() = composeTestRule.onNode(hasScrollToNodeAction())

    @Test
    fun aQuestWithARowInEverySection_rendersWithoutADuplicateKey() {
        showEverySection()

        // Composing at all is the assertion: a duplicate key throws during measure, so a broken
        // screen never gets as far as having a quest title to find.
        composeTestRule.onNodeWithText(quest.title).assertExists()
    }

    @Test
    fun scrollingThroughEverySection_doesNotCollideOnKeys() {
        showEverySection()

        // Each hop measures the sections in between, which is what reaches the duplicate key. A
        // LazyColumn composes only what is near the viewport, so without the scrolling the later
        // sections would never be measured and the test would pass against the broken code.
        for (text in listOf(
            "Search the mill",
            "50 gp",
            "The miller owes a favour",
            "She is already dead",
            "Escort Ireena to Vallaki"
        )) {
            lazyColumn().performScrollToNode(hasText(text))
            composeTestRule.onNodeWithText(text).assertExists()
        }
    }
}
