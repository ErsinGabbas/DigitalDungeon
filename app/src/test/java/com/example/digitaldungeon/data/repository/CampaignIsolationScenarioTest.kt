package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestRelationType
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
 * Scenario 3: two campaigns' quest data must never mix.
 *
 * This is the invariant the whole module rests on. A DM runs several campaigns from the same app,
 * and a quest that leaked from one into another would not read as a bug - it would read as the
 * journal being wrong about what happened at the table, which is the one thing this app exists to
 * get right. So it is pinned here from every angle a leak could come from: a read that returns too
 * much, two campaigns that happen to use the same words, an edit that moves a row across the
 * boundary, a link that joins the two sides, and a delete that reaches further than its own
 * campaign.
 *
 * Against the real repositories over a real in-memory database, because only half of the boundary
 * is code. `WHERE campaignId = :campaignId` and the two `DifferentCampaigns` checks are the
 * repositories' half; the foreign key and its `CASCADE` are SQLite's. A fake would assert the
 * first half against itself and know nothing about the second.
 *
 * Two things are deliberately not asserted here. List *order* is never asserted across more than
 * one quest: `getQuestsForCampaign` orders by `createdAt DESC`, and quests written in the same
 * millisecond - which is every quest in a test - tie. And the deep cascade below a quest
 * (objectives, notes, rewards) is not re-checked; that is the cascade scenario's job, and what
 * matters here is only that the blast radius of a delete stops at the campaign it was aimed at.
 */
@RunWith(RobolectricTestRunner::class)
class CampaignIsolationScenarioTest {

    private lateinit var env: QuestTestEnvironment

    @Before
    fun setUp() {
        env = QuestTestEnvironment()
    }

    @After
    fun tearDown() {
        env.close()
    }

    // ------------------------------------------------- reads

    @Test
    fun getQuestsForCampaign_withQuestsInTwoCampaigns_returnsOnlyTheOneAsked() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        env.quest(strahd, "Escort Ireena to Vallaki")
        env.quest(strahd, "The miller's daughter")
        env.quest(avernus, "Find Elturel")

        assertEquals(
            setOf("Escort Ireena to Vallaki", "The miller's daughter"),
            env.questTitlesIn(strahd).toSet()
        )
        assertEquals(
            listOf("Find Elturel"),
            env.questTitlesIn(avernus)
        )
    }

    @Test
    fun getQuestsForCampaign_withNoQuestsOfItsOwn_isEmptyWhileTheOtherCampaignIsNot() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        env.quest(strahd, "Escort Ireena to Vallaki")

        // An empty campaign has to stay empty. If the filter were ever dropped, this is the read
        // that would start answering with somebody else's quests instead of nothing.
        assertTrue(env.questsIn(avernus).isEmpty())
    }

    @Test
    fun getQuestsForCampaign_withTheSameTitleInBothCampaigns_returnsOneQuestEach() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        // Identical in every field the two campaigns could be confused by: same title, and both
        // take the default SIDE / NOT_STARTED. Nothing but the campaign tells them apart, which is
        // the hardest case for a boundary that is a single column.
        val inStrahd = env.quest(strahd, "The miller's daughter")
        val inAvernus = env.quest(avernus, "The miller's daughter")

        assertEquals(listOf("The miller's daughter"), env.questTitlesIn(strahd))
        assertEquals(listOf("The miller's daughter"), env.questTitlesIn(avernus))

        // Same words, two separate quests - so the ids differ and each read sees only its own.
        assertFalse(inStrahd == inAvernus)
        assertEquals(listOf(inStrahd), env.questsIn(strahd).map { it.id })
        assertEquals(listOf(inAvernus), env.questsIn(avernus).map { it.id })
    }

    @Test
    fun getQuestById_forAQuestOfEitherCampaign_carriesTheCampaignItWasWrittenWith() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "The miller's daughter")
        val inAvernus = env.quest(avernus, "The miller's daughter")

        // `campaignId` is what the quest screen hands on to every other repository it calls, so a
        // wrong one here would send a note or an objective into the wrong campaign.
        assertEquals(strahd, env.questById(inStrahd)?.campaignId)
        assertEquals(avernus, env.questById(inAvernus)?.campaignId)
    }

    // ------------------------------------------------- edits

    @Test
    fun updateQuest_renamingInOneCampaign_leavesTheOtherCampaignsQuestAlone() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "The miller's daughter")
        val inAvernus = env.quest(avernus, "The miller's daughter")

        env.quests.updateQuest(
            id = inStrahd,
            title = "The miller's daughter, found",
            description = "",
            category = QuestCategory.SIDE,
            status = QuestStatus.NOT_STARTED,
            priority = QuestPriority.NORMAL,
            isHidden = false,
            inGameDate = null
        )

        assertEquals(listOf("The miller's daughter, found"), env.questTitlesIn(strahd))

        // The update matches by id, but two quests sharing a title is exactly the shape in which a
        // title-based write would look correct right up to the moment somebody opened the other
        // campaign.
        assertEquals(listOf("The miller's daughter"), env.questTitlesIn(avernus))
        assertEquals("The miller's daughter", env.questById(inAvernus)?.title)
    }

    @Test
    fun updateQuest_changingEveryEditableField_leavesTheCampaignIdUnchanged() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "The miller's daughter")
        env.quest(avernus, "Find Elturel")

        env.quests.updateQuest(
            id = inStrahd,
            title = "Strahd's invitation",
            description = "Dinner at Ravenloft",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = true,
            inGameDate = "14th of Neth, 1492 DR"
        )

        val updated = env.questById(inStrahd)

        // Every field the edit form shows was rewritten, so if a campaign could be edited at all,
        // this is the call that would have done it. It cannot: there is no `campaignId` parameter
        // to pass, and moving a quest would drag its objectives, notes and relations across the
        // boundary with it.
        assertEquals(strahd, updated?.campaignId)

        // Asserted so the line above is not satisfied by an update that quietly did nothing.
        assertEquals("Strahd's invitation", updated?.title)
        assertEquals(QuestCategory.MAIN, updated?.category)
        assertEquals(QuestStatus.ACTIVE, updated?.status)

        assertEquals(listOf("Strahd's invitation"), env.questTitlesIn(strahd))
        assertEquals(listOf("Find Elturel"), env.questTitlesIn(avernus))
    }

    @Test
    fun updateQuestStatus_inOneCampaign_leavesTheOtherCampaignsStatusAlone() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "The miller's daughter")
        val inAvernus = env.quest(avernus, "The miller's daughter")

        env.quests.updateQuestStatus(inStrahd, QuestStatus.COMPLETED)

        assertEquals(QuestStatus.COMPLETED, env.questById(inStrahd)?.status)

        // The status write also appends a history row, so it touches two tables at once - the one
        // write in this repository where a stray id would land in two places.
        assertEquals(QuestStatus.NOT_STARTED, env.questById(inAvernus)?.status)
    }

    @Test
    fun deleteQuest_inOneCampaign_leavesTheSameTitledQuestInTheOther() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "The miller's daughter")
        val inAvernus = env.quest(avernus, "The miller's daughter")

        env.quests.deleteQuest(inStrahd)

        assertTrue(env.questsIn(strahd).isEmpty())
        assertNull(env.questById(inStrahd))

        assertEquals(listOf("The miller's daughter"), env.questTitlesIn(avernus))
        assertNotNull(env.questById(inAvernus))
    }

    // ------------------------------------------------- relations

    @Test
    fun addRelation_acrossCampaigns_isRefusedAndWritesNothing() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "Escort Ireena to Vallaki")
        val inAvernus = env.quest(avernus, "Find Elturel")

        assertEquals(
            AddRelationResult.DifferentCampaigns,
            env.relations.addRelation(inStrahd, inAvernus, QuestRelationType.PARENT_CHILD)
        )

        // The refusal has to be the specific one. `QuestMissing` or a thrown foreign key error
        // would leave the screen saying the wrong thing about a quest that is right there.
        assertTrue(env.relations.getRelationsForQuest(inStrahd).first().isEmpty())
        assertTrue(env.relations.getRelationsForQuest(inAvernus).first().isEmpty())
    }

    @Test
    fun addRelation_acrossCampaignsInEitherDirection_isRefusedForEveryRelationType() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val inStrahd = env.quest(strahd, "Escort Ireena to Vallaki")
        val inAvernus = env.quest(avernus, "Find Elturel")

        // Every type, both ways round, because two of the types are stored with the ends swapped
        // or reordered. The campaign check runs before that normalisation, and this is what says
        // so: if it ever moved after, FOLLOWS and the symmetric types would be the ones that got
        // through.
        QuestRelationType.entries.forEach { type ->

            assertEquals(
                "$type from Strahd to Avernus",
                AddRelationResult.DifferentCampaigns,
                env.relations.addRelation(inStrahd, inAvernus, type)
            )

            assertEquals(
                "$type from Avernus to Strahd",
                AddRelationResult.DifferentCampaigns,
                env.relations.addRelation(inAvernus, inStrahd, type)
            )
        }

        assertTrue(env.relations.getRelationsForQuest(inStrahd).first().isEmpty())
        assertTrue(env.relations.getRelationsForQuest(inAvernus).first().isEmpty())
    }

    @Test
    fun addRelation_withinOneCampaign_isAddedAndTheOtherCampaignSeesNothing() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val arc = env.quest(strahd, "Escort Ireena to Vallaki")
        val step = env.quest(strahd, "The miller's daughter")
        val elsewhere = env.quest(avernus, "Find Elturel")

        assertEquals(
            AddRelationResult.Added,
            env.relations.addRelation(arc, step, QuestRelationType.PARENT_CHILD)
        )

        // The control for the two tests above: the refusals there are the campaign rule, not a
        // relation repository that refuses everything.
        assertEquals(1, env.relations.getRelationsForQuest(arc).first().size)
        assertEquals(1, env.relations.getRelationsForQuest(step).first().size)

        // A relation is read from both ends at once, so a read that forgot to bound itself by
        // quest id would surface a neighbouring campaign's links here.
        assertTrue(env.relations.getRelationsForQuest(elsewhere).first().isEmpty())
    }

    // ------------------------------------------------- session links

    @Test
    fun addSessionLink_toASessionOfAnotherCampaign_isRefusedAndWritesNothing() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val questInStrahd = env.quest(strahd, "Escort Ireena to Vallaki")
        val sessionInAvernus = env.session(avernus, "Session 1")

        assertEquals(
            AddSessionLinkResult.DifferentCampaigns,
            env.sessionLinks.addLink(questId = questInStrahd, sessionId = sessionInAvernus)
        )

        // Checked from both sides. The link table is read from the quest screen and from the
        // session screen, and a row that only one of them could see would be worse than none.
        assertTrue(env.sessionLinks.getLinksForQuest(questInStrahd).first().isEmpty())
        assertTrue(env.sessionLinks.getLinksForSession(sessionInAvernus).first().isEmpty())
    }

    @Test
    fun addSessionLink_toASessionOfItsOwnCampaign_isAddedAndTheOtherCampaignSeesNothing() =
        runTest {
            val strahd = env.campaign("Curse of Strahd")
            val avernus = env.campaign("Descent into Avernus")

            val questInStrahd = env.quest(strahd, "Escort Ireena to Vallaki")
            val sessionInStrahd = env.session(strahd, "Session 3")
            val sessionInAvernus = env.session(avernus, "Session 1")

            assertEquals(
                AddSessionLinkResult.Added,
                env.sessionLinks.addLink(questId = questInStrahd, sessionId = sessionInStrahd)
            )

            assertEquals(
                listOf(sessionInStrahd),
                env.sessionLinks.getLinksForQuest(questInStrahd).first().map { it.sessionId }
            )

            // The control, and the stronger half of the assertion: one campaign's session now has
            // a link, and the other campaign's session still has none.
            assertEquals(
                listOf(questInStrahd),
                env.sessionLinks.getLinksForSession(sessionInStrahd).first().map { it.questId }
            )
            assertTrue(env.sessionLinks.getLinksForSession(sessionInAvernus).first().isEmpty())
        }

    // ------------------------------------------------- deleting a campaign

    @Test
    fun deleteCampaign_withQuestsInBoth_removesItsOwnQuestsAndLeavesTheOthers() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val ireena = env.quest(strahd, "Escort Ireena to Vallaki")
        val miller = env.quest(strahd, "The miller's daughter")
        val elturel = env.quest(avernus, "Find Elturel")
        val zariel = env.quest(avernus, "Bargain with Zariel")

        env.campaigns.deleteCampaign(strahd)

        // The cascade is the schema's, not the repository's: `deleteCampaign` deletes one row and
        // SQLite takes the quests with it.
        assertTrue(env.questsIn(strahd).isEmpty())
        assertNull(env.questById(ireena))
        assertNull(env.questById(miller))

        // The half that makes this an isolation test rather than a cascade one: the blast radius
        // stopped at the campaign it was aimed at.
        assertEquals(
            setOf("Find Elturel", "Bargain with Zariel"),
            env.questTitlesIn(avernus).toSet()
        )
        assertNotNull(env.questById(elturel))
        assertNotNull(env.questById(zariel))
    }

    @Test
    fun deleteCampaign_withLinkedQuestsInBoth_leavesTheOtherCampaignsLinksIntact() = runTest {
        val strahd = env.campaign("Curse of Strahd")
        val avernus = env.campaign("Descent into Avernus")

        val arc = env.quest(strahd, "Escort Ireena to Vallaki")
        val step = env.quest(strahd, "The miller's daughter")
        env.relations.addRelation(arc, step, QuestRelationType.PARENT_CHILD)

        val elturel = env.quest(avernus, "Find Elturel")
        val zariel = env.quest(avernus, "Bargain with Zariel")
        env.relations.addRelation(elturel, zariel, QuestRelationType.PRECEDES)

        val sessionInAvernus = env.session(avernus, "Session 1")
        env.sessionLinks.addLink(questId = elturel, sessionId = sessionInAvernus)

        env.campaigns.deleteCampaign(strahd)

        // Relations and session links hang off quest ids, not campaign ids, so the cascade reaches
        // them one step removed. That indirection is where a surviving campaign could lose rows
        // that were never pointed at the deleted one.
        assertEquals(1, env.relations.getRelationsForQuest(elturel).first().size)
        assertEquals(1, env.relations.getRelationsForQuest(zariel).first().size)
        assertEquals(
            listOf(elturel),
            env.sessionLinks.getLinksForSession(sessionInAvernus).first().map { it.questId }
        )

        assertTrue(env.relations.getRelationsForQuest(arc).first().isEmpty())
        assertEquals(listOf("Session 1"), env.sessionsIn(avernus).map { it.title })
    }
}
