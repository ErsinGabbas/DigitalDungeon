package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.testing.QuestTestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The life of a quest, from one line of text to reopened after it was finished: scenarios 1, 2, 4
 * and 5 of the thirteen.
 *
 * Run against the real [QuestRepositoryImpl] over a real in-memory database, because every claim
 * made here is a claim about what ends up in the rows. The defaults of `addQuest` are a promise to
 * the user ("writing down a quest costs one line"), and a promise kept only by the interface's
 * default arguments would still be broken if the implementation wrote something else into the
 * entity. Likewise "deleting one quest leaves the others alone" is a statement about a `DELETE
 * WHERE id`, and only SQLite can be asked whether that is what happened.
 *
 * What is asserted and why:
 *  - the seven fields `addQuest` fills in when it is given two, one by one rather than by comparing
 *    a whole [com.example.digitaldungeon.domain.model.Quest]: `id` and `createdAt` are assigned by
 *    the insert and cannot be written down in advance, and a comparison that excluded them would
 *    have to list the other seven anyway.
 *  - that an edit rewrites every editable field *and* keeps `campaignId` and `createdAt`, which the
 *    update statement deliberately omits - a quest that changed campaign on an edit would drag its
 *    objectives and relations across the boundary that keeps two campaigns apart.
 *  - that every status is reachable from every other, including out of COMPLETED and FAILED. This
 *    is a product rule and not an accident of the implementation: a quest everyone thought was over
 *    is an ordinary turn of a campaign, so these tests exist to fail the day someone adds a guard.
 *  - that each status move leaves exactly one history line naming the new status, since the log is
 *    only worth reading if it is neither short nor doubled.
 *
 * What is *not* asserted: the order of two quests created in the same millisecond. The DAO orders
 * by `createdAt DESC` and `addQuest` stamps `System.currentTimeMillis()`, so three quests written
 * in a row can share a timestamp and SQLite owes no tie-break. The ordering claim made below is
 * the one the query actually makes - timestamps come out non-increasing - and membership is
 * asserted as a set.
 */
@RunWith(RobolectricTestRunner::class)
class QuestLifecycleScenarioTest {

    private lateinit var env: QuestTestEnvironment

    @Before
    fun setUp() {
        env = QuestTestEnvironment()
    }

    @After
    fun tearDown() {
        env.close()
    }

    // ------------------------------------------------------------- creating

    @Test
    fun addQuest_withOnlyACampaignAndATitle_fillsInEveryOtherFieldWithItsDefault() = runTest {

        val campaignId = env.campaign()

        env.quests.addQuest(campaignId = campaignId, title = "The miller wants his daughter back")

        val quest = env.questsIn(campaignId).single()
        assertEquals("The miller wants his daughter back", quest.title)
        assertEquals(campaignId, quest.campaignId)
        assertEquals("", quest.description)
        assertEquals(QuestCategory.SIDE, quest.category)
        // NOT_STARTED is the honest starting point: a quest just written down has not begun.
        assertEquals(QuestStatus.NOT_STARTED, quest.status)
        assertEquals(QuestPriority.NORMAL, quest.priority)
        assertFalse(quest.isHidden)
        assertNull(quest.inGameDate)
    }

    @Test
    fun addQuest_withOnlyACampaignAndATitle_writesNoHistoryEntry() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "The miller wants his daughter back")

        // Creation is not a thing that happened *to* the quest, and the quest itself already says
        // when it was written down. It is also what lets the status tests count entries: an entry
        // written by `addQuest` would offset every one of those totals by one.
        assertTrue(env.history.getHistoryForQuest(questId).first().isEmpty())
    }

    @Test
    fun addQuest_calledThreeTimesForOneCampaign_keepsAllThreeWithIdsOfTheirOwn() = runTest {

        val campaignId = env.campaign()

        env.quest(campaignId, "The miller wants his daughter back")
        env.quest(campaignId, "Escort the wagon to Vallaki")
        env.quest(campaignId, "Find out who owns the windmill")

        val quests = env.questsIn(campaignId)
        assertEquals(3, quests.size)
        assertEquals(
            setOf(
                "The miller wants his daughter back",
                "Escort the wagon to Vallaki",
                "Find out who owns the windmill"
            ),
            quests.map { it.title }.toSet()
        )
        // Three distinct ids, because every later call - an edit, a status move, a delete - names a
        // quest by id and would otherwise hit the wrong one.
        assertEquals(3, quests.map { it.id }.toSet().size)
    }

    @Test
    fun getQuestsForCampaign_withSeveralQuests_answersNewestFirst() = runTest {

        val campaignId = env.campaign()
        env.quest(campaignId, "First written down")
        env.quest(campaignId, "Second written down")
        env.quest(campaignId, "Third written down")

        val createdAt = env.questsIn(campaignId).map { it.createdAt }

        // Non-increasing and not strictly decreasing: three inserts can land inside one
        // millisecond, and the query promises an order by timestamp, not that they differ.
        assertEquals(createdAt.sortedDescending(), createdAt)
    }

    @Test
    fun getQuestsForCampaign_forACampaignWithNoQuests_answersEmpty() = runTest {

        val campaignId = env.campaign()

        // Needed to tell "this campaign has nothing yet" apart from "nothing matched a filter",
        // which is the one thing the unfiltered list is for.
        assertTrue(env.questsIn(campaignId).isEmpty())
    }

    // -------------------------------------------------------------- editing

    @Test
    fun updateQuest_withEveryFieldChanged_rewritesThemAll() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "The miller wants his daughter back")

        env.quests.updateQuest(
            id = questId,
            title = "Bring Gertruda home",
            description = "She followed a voice into the mists.",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = true,
            inGameDate = "14th of Neth, 1492 DR"
        )

        val quest = env.questById(questId)
        assertNotNull(quest)
        assertEquals("Bring Gertruda home", quest!!.title)
        assertEquals("She followed a voice into the mists.", quest.description)
        assertEquals(QuestCategory.MAIN, quest.category)
        assertEquals(QuestStatus.ACTIVE, quest.status)
        assertEquals(QuestPriority.HIGH, quest.priority)
        assertTrue(quest.isHidden)
        assertEquals("14th of Neth, 1492 DR", quest.inGameDate)
    }

    @Test
    fun updateQuest_withEveryFieldChanged_keepsTheCampaignAndTheCreationTime() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "The miller wants his daughter back")
        val before = env.questById(questId)

        env.quests.updateQuest(
            id = questId,
            title = "Bring Gertruda home",
            description = "She followed a voice into the mists.",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = true,
            inGameDate = "14th of Neth, 1492 DR"
        )

        val after = env.questById(questId)
        // The update statement names neither column. When a quest was written down is a fact, and a
        // quest that could change campaign on an edit would carry its objectives and relations
        // across the one boundary this app keeps.
        assertEquals(campaignId, after?.campaignId)
        assertEquals(before?.createdAt, after?.createdAt)
        assertEquals(questId, after?.id)
    }

    @Test
    fun updateQuest_onOneQuest_leavesTheOtherQuestsUntouched() = runTest {

        val campaignId = env.campaign()
        val editedId = env.quest(campaignId, "The miller wants his daughter back")
        val untouchedId = env.quest(campaignId, "Escort the wagon to Vallaki")
        val untouchedBefore = env.questById(untouchedId)

        env.quests.updateQuest(
            id = editedId,
            title = "Bring Gertruda home",
            description = "She followed a voice into the mists.",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = true,
            inGameDate = "14th of Neth, 1492 DR"
        )

        // Compared whole: the `WHERE id` is the only thing keeping an edit from being a bulk write,
        // and a field-by-field check would be the field-by-field check of the test above again.
        assertEquals(untouchedBefore, env.questById(untouchedId))
        assertEquals(2, env.questsIn(campaignId).size)
    }

    // ------------------------------------------------------------- deleting

    @Test
    fun deleteQuest_withSeveralQuestsInTheCampaign_removesExactlyThatOne() = runTest {

        val campaignId = env.campaign()
        env.quest(campaignId, "The miller wants his daughter back")
        val doomedId = env.quest(campaignId, "Escort the wagon to Vallaki")
        env.quest(campaignId, "Find out who owns the windmill")

        env.quests.deleteQuest(doomedId)

        assertNull(env.questById(doomedId))
        assertEquals(
            setOf(
                "The miller wants his daughter back",
                "Find out who owns the windmill"
            ),
            env.questTitlesIn(campaignId).toSet()
        )
    }

    @Test
    fun deleteQuest_ofOneQuest_leavesTheCampaignAndItsOtherQuestsAlone() = runTest {

        val campaignId = env.campaign("Curse of Strahd")
        val keptId = env.quest(campaignId, "The miller wants his daughter back")
        val doomedId = env.quest(campaignId, "Escort the wagon to Vallaki")
        val keptBefore = env.questById(keptId)

        env.quests.deleteQuest(doomedId)

        // The quest hangs off the campaign and not the other way round, so deleting a quest must
        // not reach upwards - the cascade runs one direction only.
        assertEquals("Curse of Strahd", env.campaigns.getCampaignById(campaignId)?.name)
        assertEquals(keptBefore, env.questById(keptId))
    }

    // --------------------------------------------------------------- status

    @Test
    fun updateQuestStatus_walkingAQuestToCompletedAndBackToActive_acceptsEveryMove() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Bring Gertruda home")
        assertEquals(QuestStatus.NOT_STARTED, env.questById(questId)?.status)

        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)
        assertEquals(QuestStatus.ACTIVE, env.questById(questId)?.status)

        env.quests.updateQuestStatus(questId, QuestStatus.COMPLETED)
        assertEquals(QuestStatus.COMPLETED, env.questById(questId)?.status)

        // The move this test exists for. Nothing may refuse it: a quest everyone thought was over
        // turning out not to be is an ordinary turn of a campaign, and the DM judges that, not the
        // repository.
        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)
        assertEquals(QuestStatus.ACTIVE, env.questById(questId)?.status)
    }

    @Test
    fun updateQuestStatus_onAFailedQuest_reopensItAsActive() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Warn Vallaki before the feast")
        env.quests.updateQuestStatus(questId, QuestStatus.FAILED)

        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)

        // FAILED is no more terminal than COMPLETED. A party that failed to warn the town can still
        // be given the chance again, and the data has to allow what the story allows.
        assertEquals(QuestStatus.ACTIVE, env.questById(questId)?.status)
    }

    @Test
    fun updateQuestStatus_everyMove_appendsOneHistoryEntryNamingTheNewStatus() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Bring Gertruda home")

        val walk = listOf(
            QuestStatus.ACTIVE,
            QuestStatus.COMPLETED,
            QuestStatus.ACTIVE,
            QuestStatus.FAILED,
            QuestStatus.ACTIVE
        )

        walk.forEachIndexed { movesMade, status ->

            env.quests.updateQuestStatus(questId, status)

            val history = env.history.getHistoryForQuest(questId).first()
            // Checked after each move rather than once at the end, because the log is read newest
            // first and a walk that revisits ACTIVE would make a single comparison of the finished
            // list read the same forwards as backwards.
            assertEquals(movesMade + 1, history.size)

            val latest = history.first()
            assertEquals(questId, latest.questId)
            assertEquals(QuestHistoryEventType.QUEST_STATUS_CHANGED, latest.eventType)
            // The constant name and not a sentence: the log is rendered in whatever language the
            // app is in when it is read, not the one it was written in.
            assertEquals(status.name, latest.description)
        }
    }

    @Test
    fun updateQuestStatus_withTheStatusTheQuestAlreadyHas_logsItAnyway() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Bring Gertruda home")
        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)

        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)

        // Deliberate, and the layer matters: suppressing a no-op write is a decision about what the
        // UI should bother asking for, and the quest screen already makes it. A repository that
        // silently dropped a write it was told to make would be the surprising one.
        assertEquals(2, env.history.getHistoryForQuest(questId).first().size)
    }

    @Test
    fun updateQuestStatus_onOneQuest_leavesTheOtherQuestsStatusAndHistoryAlone() = runTest {

        val campaignId = env.campaign()
        val movedId = env.quest(campaignId, "Bring Gertruda home")
        val untouchedId = env.quest(campaignId, "Escort the wagon to Vallaki")

        env.quests.updateQuestStatus(movedId, QuestStatus.COMPLETED)

        assertEquals(QuestStatus.NOT_STARTED, env.questById(untouchedId)?.status)
        // The history is written in the same transaction as the status, so a write that reached the
        // wrong quest would be visible here as well as above.
        assertTrue(env.history.getHistoryForQuest(untouchedId).first().isEmpty())
    }
}
