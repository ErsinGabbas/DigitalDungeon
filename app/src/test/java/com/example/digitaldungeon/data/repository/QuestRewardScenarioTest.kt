package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestConsequence
import com.example.digitaldungeon.domain.model.QuestConsequenceType
import com.example.digitaldungeon.domain.model.QuestNote
import com.example.digitaldungeon.domain.model.QuestReward
import com.example.digitaldungeon.domain.model.QuestRewardType
import com.example.digitaldungeon.testing.QuestTestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Scenario 8: what hangs off a quest once it is written down - its rewards, its consequences and the
 * DM's notes on it. Real repositories, real SQLite, no fakes.
 *
 * The three lists are deliberately tested together because they are the same shape: a list keyed by
 * `questId`, append-only, with add, update and delete and nothing else. Splitting them across three
 * files would triple the setup and hide the one thing worth seeing side by side, which is that all
 * three behave identically.
 *
 * Why these assertions and not others:
 *  - The **defaults** are asserted because the add form lets the DM type a title and stop there, and
 *    a default that arrived as `null` instead of `""` would surface as a crash or a blank row far
 *    away from here. The type defaults matter for the same reason: `OTHER` is the drawer a reward
 *    with no stated kind belongs in, not an absent value.
 *  - **Every enum constant** is stored and read back, because Room persists these as TEXT by
 *    constant name. A converter that handled four of five, or a name that stopped round-tripping,
 *    would be invisible to a test that only ever used one constant.
 *  - **Update** is asserted on all three fields at once, because the repository rewrites the row as
 *    a whole and the failure worth catching is one field being quietly left behind.
 *  - **Delete** is asserted on its siblings and not just on the deleted row: `DELETE ... WHERE id`
 *    with a wrong predicate still makes the target disappear, and the only way to notice is to count
 *    what survived.
 *  - **Emptiness is a valid state.** A simple quest must be creatable without filling in any
 *    optional part, so a quest with no rewards, no consequences and no notes is a requirement and
 *    not an accident waiting to be fixed.
 *  - **Scoping by quest.** Two quests in the *same* campaign, so what is under test is the `questId`
 *    predicate rather than campaign isolation, which another file covers.
 *
 * Note ordering is asserted as a set, not a sequence. `getNotesForQuest` orders by `createdAt DESC`,
 * and the repository stamps that from the system clock, so notes written one after another inside a
 * test land in the same millisecond. Asserting newest-first here would be asserting the tie-break,
 * which is a different claim from the one the ordering is for.
 */
@RunWith(RobolectricTestRunner::class)
class QuestRewardScenarioTest {

    private lateinit var env: QuestTestEnvironment

    @Before
    fun setUp() {
        env = QuestTestEnvironment()
    }

    @After
    fun tearDown() {
        env.close()
    }

    // ------------------------------------------------- helpers

    /** A campaign with a single quest in it, which is all most of these tests need. */
    private suspend fun aQuest(title: String = "Escort the wagon to Vallaki"): Int =
        env.quest(env.campaign(), title)

    private suspend fun rewardsOf(questId: Int): List<QuestReward> =
        env.rewards.getRewardsForQuest(questId).first()

    private suspend fun consequencesOf(questId: Int): List<QuestConsequence> =
        env.consequences.getConsequencesForQuest(questId).first()

    private suspend fun notesOf(questId: Int): List<QuestNote> =
        env.notes.getNotesForQuest(questId).first()

    // Every `add` answers with Unit, so the id of a just-written row has to be found by the text it
    // was written with. `single` rather than `first`, so a test that accidentally wrote the same
    // row twice fails here instead of silently asserting about one of two.
    private suspend fun rewardTitled(questId: Int, title: String): QuestReward =
        rewardsOf(questId).single { it.title == title }

    private suspend fun consequenceTitled(questId: Int, title: String): QuestConsequence =
        consequencesOf(questId).single { it.title == title }

    private suspend fun noteSaying(questId: Int, content: String): QuestNote =
        notesOf(questId).single { it.content == content }

    // ------------------------------------------------- the empty quest

    @Test
    fun aFreshQuest_hasNoRewardsNoConsequencesAndNoNotes() = runTest {
        val questId = aQuest()

        // Not a gap to be filled in later: a quest that promises nothing, broke nothing and needed
        // no notes is a finished, ordinary quest, and all three queries must answer with an empty
        // list rather than failing or waiting for something.
        assertEquals(emptyList<QuestReward>(), rewardsOf(questId))
        assertEquals(emptyList<QuestConsequence>(), consequencesOf(questId))
        assertEquals(emptyList<QuestNote>(), notesOf(questId))
    }

    // ------------------------------------------------- rewards

    @Test
    fun addReward_withOnlyATitle_storesTheOtherTwoFieldsAsTheirDefaults() = runTest {
        val questId = aQuest()

        env.rewards.addReward(questId = questId, title = "A purse of gold")

        val reward = rewardsOf(questId).single()
        assertEquals("A purse of gold", reward.title)
        assertEquals("", reward.description)
        assertEquals(QuestRewardType.OTHER, reward.type)
        assertEquals(questId, reward.questId)
    }

    @Test
    fun addReward_withEveryType_storesAndReadsBackEachConstant() = runTest {
        val questId = aQuest()

        // Titled by constant name so the round trip can be checked per constant: Room writes these
        // as TEXT, and a name that fails to map back would otherwise hide behind a passing test
        // that only ever used OTHER.
        QuestRewardType.entries.forEach { type ->
            env.rewards.addReward(
                questId = questId,
                title = type.name,
                description = "stored as ${type.name}",
                type = type
            )
        }

        val stored = rewardsOf(questId).associate { it.title to it.type }
        assertEquals(
            QuestRewardType.entries.associate { it.name to it },
            stored
        )
    }

    @Test
    fun updateReward_rewritesTheTitleTheDescriptionAndTheType() = runTest {
        val questId = aQuest()
        env.rewards.addReward(
            questId = questId,
            title = "A purse of gold",
            description = "Fifty gold pieces.",
            type = QuestRewardType.MONEY
        )
        val before = rewardTitled(questId, "A purse of gold")

        env.rewards.updateReward(
            id = before.id,
            title = "The Baron's favour",
            description = "He will hear the party out once, and once only.",
            type = QuestRewardType.REPUTATION
        )

        val after = rewardsOf(questId).single()
        assertEquals("The Baron's favour", after.title)
        assertEquals("He will hear the party out once, and once only.", after.description)
        assertEquals(QuestRewardType.REPUTATION, after.type)
        // Same row rewritten, not a new row written next to the old one.
        assertEquals(before.id, after.id)
        assertEquals(questId, after.questId)
    }

    @Test
    fun deleteReward_removesThatRewardAndLeavesItsSiblings() = runTest {
        val questId = aQuest()
        env.rewards.addReward(questId = questId, title = "A purse of gold")
        env.rewards.addReward(questId = questId, title = "The Baron's favour")
        env.rewards.addReward(questId = questId, title = "Sunsword")
        val doomed = rewardTitled(questId, "The Baron's favour")

        env.rewards.deleteReward(doomed.id)

        assertEquals(
            listOf("A purse of gold", "Sunsword"),
            rewardsOf(questId).map { it.title }
        )
    }

    // ------------------------------------------------- consequences

    @Test
    fun addConsequence_withOnlyATitle_storesTheOtherTwoFieldsAsTheirDefaults() = runTest {
        val questId = aQuest()

        env.consequences.addConsequence(questId = questId, title = "The mill burned down")

        val consequence = consequencesOf(questId).single()
        assertEquals("The mill burned down", consequence.title)
        assertEquals("", consequence.description)
        assertEquals(QuestConsequenceType.OTHER, consequence.type)
        assertEquals(questId, consequence.questId)
    }

    @Test
    fun addConsequence_withEveryType_storesAndReadsBackEachConstant() = runTest {
        val questId = aQuest()

        QuestConsequenceType.entries.forEach { type ->
            env.consequences.addConsequence(
                questId = questId,
                title = type.name,
                description = "stored as ${type.name}",
                type = type
            )
        }

        val stored = consequencesOf(questId).associate { it.title to it.type }
        assertEquals(
            QuestConsequenceType.entries.associate { it.name to it },
            stored
        )
    }

    @Test
    fun updateConsequence_rewritesTheTitleTheDescriptionAndTheType() = runTest {
        val questId = aQuest()
        env.consequences.addConsequence(
            questId = questId,
            title = "The mill burned down",
            description = "Nobody was inside.",
            type = QuestConsequenceType.WORLD
        )
        val before = consequenceTitled(questId, "The mill burned down")

        env.consequences.updateConsequence(
            id = before.id,
            title = "The Vallaki guard wants the party gone",
            description = "Izek has their descriptions.",
            type = QuestConsequenceType.FACTION
        )

        val after = consequencesOf(questId).single()
        assertEquals("The Vallaki guard wants the party gone", after.title)
        assertEquals("Izek has their descriptions.", after.description)
        assertEquals(QuestConsequenceType.FACTION, after.type)
        assertEquals(before.id, after.id)
        assertEquals(questId, after.questId)
    }

    @Test
    fun deleteConsequence_removesThatConsequenceAndLeavesItsSiblings() = runTest {
        val questId = aQuest()
        env.consequences.addConsequence(questId = questId, title = "The mill burned down")
        env.consequences.addConsequence(questId = questId, title = "The miller is dead")
        env.consequences.addConsequence(questId = questId, title = "The Keepers owe a debt")
        val doomed = consequenceTitled(questId, "The miller is dead")

        env.consequences.deleteConsequence(doomed.id)

        assertEquals(
            listOf("The mill burned down", "The Keepers owe a debt"),
            consequencesOf(questId).map { it.title }
        )
    }

    // ------------------------------------------------- notes

    @Test
    fun addNote_withNothingButContent_stampsBothTimestampsToTheSameInstant() = runTest {
        val questId = aQuest()

        env.notes.addNote(questId = questId, content = "The miller is lying about the well.")

        val note = notesOf(questId).single()
        assertEquals("The miller is lying about the well.", note.content)
        assertEquals(questId, note.questId)
        // Equal timestamps are how an untouched note is told apart from a revised one, so a fresh
        // note must not arrive already looking edited.
        assertEquals(note.createdAt, note.updatedAt)
        assertTrue(note.createdAt > 0L)
    }

    @Test
    fun getNotesForQuest_answersWithEveryNoteOfThatQuest() = runTest {
        val questId = aQuest()

        env.notes.addNote(questId = questId, content = "The miller is lying about the well.")
        env.notes.addNote(questId = questId, content = "Ireena has not slept in three days.")
        env.notes.addNote(questId = questId, content = "The Vistani took the sword east.")

        // A set, because all three were written in the same millisecond and their relative order is
        // then down to the id tie-break rather than to the `createdAt DESC` the ordering exists for.
        assertEquals(
            setOf(
                "The miller is lying about the well.",
                "Ireena has not slept in three days.",
                "The Vistani took the sword east."
            ),
            notesOf(questId).map { it.content }.toSet()
        )
    }

    @Test
    fun updateNote_rewritesTheContentAndLeavesCreatedAtWhereItWas() = runTest {
        val questId = aQuest()
        env.notes.addNote(questId = questId, content = "The miller is lying about the well.")
        val before = noteSaying(questId, "The miller is lying about the well.")

        env.notes.updateNote(
            id = before.id,
            content = "The miller was lying about the well - it is where the body went."
        )

        val after = notesOf(questId).single()
        assertEquals(
            "The miller was lying about the well - it is where the body went.",
            after.content
        )
        assertEquals(before.id, after.id)
        // When the thought was first written down is a fact about the past, and it is also what the
        // list is ordered by, so a reworded note has to keep its place.
        assertEquals(before.createdAt, after.createdAt)
        // Only `>=`: the edit can land inside the same millisecond as the insert, so a strictly
        // later stamp is not something this test can honestly demand.
        assertTrue(after.updatedAt >= after.createdAt)
    }

    @Test
    fun deleteNote_removesThatNoteAndLeavesItsSiblings() = runTest {
        val questId = aQuest()
        env.notes.addNote(questId = questId, content = "The miller is lying about the well.")
        env.notes.addNote(questId = questId, content = "Ireena has not slept in three days.")
        env.notes.addNote(questId = questId, content = "The Vistani took the sword east.")
        val doomed = noteSaying(questId, "Ireena has not slept in three days.")

        env.notes.deleteNote(doomed.id)

        assertEquals(
            setOf(
                "The miller is lying about the well.",
                "The Vistani took the sword east."
            ),
            notesOf(questId).map { it.content }.toSet()
        )
    }

    // ------------------------------------------------- one quest at a time

    @Test
    fun addReward_toOneQuest_leavesTheOtherQuestsRewardsEmpty() = runTest {
        // Both quests in the same campaign, so a passing test says the `questId` predicate works and
        // not merely that two campaigns happen to be stored apart.
        val campaignId = env.campaign()
        val escort = env.quest(campaignId, "Escort the wagon to Vallaki")
        val windmill = env.quest(campaignId, "Burn out the windmill")

        env.rewards.addReward(questId = escort, title = "A purse of gold")

        assertEquals(listOf("A purse of gold"), rewardsOf(escort).map { it.title })
        assertEquals(emptyList<QuestReward>(), rewardsOf(windmill))
    }

    @Test
    fun addConsequence_toOneQuest_leavesTheOtherQuestsConsequencesEmpty() = runTest {
        val campaignId = env.campaign()
        val escort = env.quest(campaignId, "Escort the wagon to Vallaki")
        val windmill = env.quest(campaignId, "Burn out the windmill")

        env.consequences.addConsequence(questId = escort, title = "The wagon never arrived")

        assertEquals(
            listOf("The wagon never arrived"),
            consequencesOf(escort).map { it.title }
        )
        assertEquals(emptyList<QuestConsequence>(), consequencesOf(windmill))
    }

    @Test
    fun addNote_toOneQuest_leavesTheOtherQuestsNotesEmpty() = runTest {
        val campaignId = env.campaign()
        val escort = env.quest(campaignId, "Escort the wagon to Vallaki")
        val windmill = env.quest(campaignId, "Burn out the windmill")

        env.notes.addNote(questId = escort, content = "The driver wants paying up front.")

        assertEquals(
            listOf("The driver wants paying up front."),
            notesOf(escort).map { it.content }
        )
        assertEquals(emptyList<QuestNote>(), notesOf(windmill))
    }
}
