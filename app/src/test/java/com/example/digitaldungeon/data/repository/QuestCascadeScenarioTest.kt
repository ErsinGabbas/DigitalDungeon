package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestRelationType
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.testing.QuestTestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Scenario 11: deleting a quest that has everything hanging off it.
 *
 * The specification's requirement is "no dangling references after a delete", and that is not a
 * requirement about one list being empty - it is a requirement about the whole database. So these
 * tests check every table through the backup DAOs' unscoped reads rather than through the per-quest
 * queries: a per-quest query would answer "nothing for this quest" even if the row were still
 * sitting there pointing at a quest that no longer exists.
 *
 * Nothing here is implemented in Kotlin. Every assertion is about the `ON DELETE CASCADE` clauses in
 * the entities, which is the point: the integrity is a property of the schema, so it holds for every
 * caller, including a future one that forgets about it. A test that asserted it over fakes would be
 * asserting nothing at all.
 */
@RunWith(RobolectricTestRunner::class)
class QuestCascadeScenarioTest {

    private lateinit var env: QuestTestEnvironment

    @Before
    fun setUp() {
        env = QuestTestEnvironment()
    }

    @After
    fun tearDown() {
        env.close()
    }

    /**
     * A quest with one of everything attached, plus a second quest to be related to it.
     *
     * Returns both ids. The caller deletes one and asserts about the other, which is the only way to
     * tell a cascade from a `DELETE FROM`.
     */
    private suspend fun questWithEverything(): Pair<Int, Int> {

        val campaignId = env.campaign()
        val sessionId = env.session(campaignId)

        val questId = env.quest(campaignId, "The miller's missing daughter")
        val otherQuestId = env.quest(campaignId, "Escort Ireena to Vallaki")

        val objectiveId = env.objective(questId, "Search the mill")
        env.objectives.updateObjectiveStatus(objectiveId, QuestObjectiveStatus.COMPLETED)
        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)

        env.rewards.addReward(questId = questId, title = "50 gp")
        env.consequences.addConsequence(questId = questId, title = "The miller owes a favour")
        env.notes.addNote(questId = questId, content = "She is already dead")

        assertEquals(
            AddRelationResult.Added,
            env.relations.addRelation(questId, otherQuestId, QuestRelationType.PARENT_CHILD)
        )
        assertEquals(
            AddSessionLinkResult.Added,
            env.sessionLinks.addLink(questId = questId, sessionId = sessionId)
        )

        return questId to otherQuestId
    }

    // ------------------------------------------------- deleting one quest

    @Test
    fun deleteQuest_withObjectivesRewardsConsequencesAndNotes_takesAllOfThemWithIt() = runTest {

        val (questId, _) = questWithEverything()

        // Proof the setup wrote something, so the assertions below cannot pass vacuously.
        assertEquals(1, env.database.questObjectiveDao().getAllObjectives().size)
        assertEquals(1, env.database.questRewardDao().getAllRewards().size)
        assertEquals(1, env.database.questConsequenceDao().getAllConsequences().size)
        assertEquals(1, env.database.questNoteDao().getAllNotes().size)

        env.quests.deleteQuest(questId)

        assertTrue(env.database.questObjectiveDao().getAllObjectives().isEmpty())
        assertTrue(env.database.questRewardDao().getAllRewards().isEmpty())
        assertTrue(env.database.questConsequenceDao().getAllConsequences().isEmpty())
        assertTrue(env.database.questNoteDao().getAllNotes().isEmpty())
    }

    @Test
    fun deleteQuest_withHistoryAndASessionLink_takesThoseTooAndLeavesTheSession() = runTest {

        val (questId, _) = questWithEverything()

        val campaignId = env.database.campaignDao().getAllCampaigns().single().id

        assertTrue(env.database.questHistoryDao().getAllEntries().isNotEmpty())
        assertEquals(1, env.database.questSessionLinkDao().getAllLinks().size)

        env.quests.deleteQuest(questId)

        assertTrue(env.database.questHistoryDao().getAllEntries().isEmpty())
        assertTrue(env.database.questSessionLinkDao().getAllLinks().isEmpty())
        // The session was only ever referenced by the link, so it must survive untouched: a quest
        // being deleted says nothing about whether the evening happened.
        assertEquals(1, env.sessionsIn(campaignId).size)
    }

    @Test
    fun deleteQuest_atOneEndOfARelation_removesTheRelationAndKeepsTheQuestAtTheOtherEnd() = runTest {

        val (questId, otherQuestId) = questWithEverything()

        env.quests.deleteQuest(questId)

        assertTrue(env.database.questRelationDao().getAllRelations().isEmpty())
        // The other quest is independent, which is the whole reason relations are a separate table
        // and not a parent column.
        assertEquals("Escort Ireena to Vallaki", env.questById(otherQuestId)?.title)
    }

    @Test
    fun deleteQuest_leavesTheOtherQuestsPartsAlone() = runTest {

        val (questId, otherQuestId) = questWithEverything()

        env.rewards.addReward(questId = otherQuestId, title = "Ireena's gratitude")
        env.objectives.addObjective(questId = otherQuestId, title = "Leave Barovia")

        env.quests.deleteQuest(questId)

        assertEquals(
            listOf("Ireena's gratitude"),
            env.rewards.getRewardsForQuest(otherQuestId).first().map { it.title }
        )
        assertEquals(
            listOf("Leave Barovia"),
            env.objectives.getObjectivesForQuest(otherQuestId).first().map { it.title }
        )
    }

    // ------------------------------------------------- deleting the campaign

    @Test
    fun deleteCampaign_withQuestsAndSessions_emptiesEveryTableBelowIt() = runTest {

        questWithEverything()

        val campaignId = env.database.campaignDao().getAllCampaigns().single().id

        env.campaigns.deleteCampaign(campaignId)

        // The three-level cascade: campaign -> quest -> the quest's parts. Every table is named
        // here on purpose. A new table added to the schema without a cascade would leave rows
        // behind, and this is the test that would notice.
        assertTrue(env.database.campaignDao().getAllCampaigns().isEmpty())
        assertTrue(env.database.campaignParticipantDao().getAllParticipants().isEmpty())
        assertTrue(env.database.sessionDao().getAllSessions().isEmpty())
        assertTrue(env.database.sessionEventDao().getAllEvents().isEmpty())
        assertTrue(env.database.questDao().getAllQuests().isEmpty())
        assertTrue(env.database.questObjectiveDao().getAllObjectives().isEmpty())
        assertTrue(env.database.questRewardDao().getAllRewards().isEmpty())
        assertTrue(env.database.questConsequenceDao().getAllConsequences().isEmpty())
        assertTrue(env.database.questNoteDao().getAllNotes().isEmpty())
        assertTrue(env.database.questRelationDao().getAllRelations().isEmpty())
        assertTrue(env.database.questHistoryDao().getAllEntries().isEmpty())
        assertTrue(env.database.questSessionLinkDao().getAllLinks().isEmpty())
    }

    @Test
    fun deleteSession_withAQuestLinkedToIt_removesTheLinkAndKeepsTheQuest() = runTest {

        val (questId, _) = questWithEverything()

        val campaignId = env.database.campaignDao().getAllCampaigns().single().id
        val sessionId = env.sessionsIn(campaignId).single().id

        env.sessions.deleteSession(sessionId)

        assertTrue(env.database.questSessionLinkDao().getAllLinks().isEmpty())
        // The link cascades from both ends, and from either end the other side is untouched: the
        // quest outlives the session it came up in, which is the normal case.
        assertEquals("The miller's missing daughter", env.questById(questId)?.title)
    }

    @Test
    fun deleteQuest_removesTheQuestItself_andNothingElseInTheCampaign() = runTest {

        val (questId, otherQuestId) = questWithEverything()

        val campaignId = env.database.campaignDao().getAllCampaigns().single().id

        env.quests.deleteQuest(questId)

        assertNull(env.questById(questId))
        assertEquals(listOf(otherQuestId), env.questsIn(campaignId).map { it.id })
        assertEquals(1, env.database.campaignDao().getAllCampaigns().size)
    }
}
