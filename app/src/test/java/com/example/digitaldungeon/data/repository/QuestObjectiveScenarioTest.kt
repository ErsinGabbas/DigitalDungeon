package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.QuestHistoryEntry
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.testing.QuestTestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Scenarios 6 and 7 of the specification - sequential and parallel objectives, and optional ones -
 * against the real repositories over a real database.
 *
 * The specification asks for "sequential and parallel" objectives, and the app answers with an
 * ordered list and nothing else. "Sequential" is the order: `orderIndex`, the list coming back in
 * it, and `moveObjective` as the one way to arrange it. "Parallel" is the *absence* of machinery:
 * no prerequisites, no flag, no gate that stops the third step being ticked while the first is
 * untouched. So the parallel half cannot be asserted by looking for a feature - it is asserted by
 * showing that completing steps out of order is accepted and leaves everything else where it was.
 * Inventing an `isParallel` column to have something to assert would be inventing the feature.
 *
 * Optional objectives are the same kind of answer: `isOptional` is a marker the DM reads, so the
 * assertions are that it survives a round trip and that an optional step is otherwise listed,
 * ordered and moved exactly like a required one.
 *
 * The hard rule the whole file exists to pin down: completing every objective does not touch the
 * quest's status. Whether the miller's daughter being found ends the quest is a judgement about the
 * story, and no code in this app makes it. That is cheap to believe today and easy to break by
 * accident, which is why it is asserted rather than documented.
 *
 * Real repositories and real SQLite rather than fakes, because every claim here - the ORDER BY, the
 * renumbering transaction, the history row written beside the status - belongs to the database or
 * to the production implementation. A fake would assert that the test's own list behaves.
 */
@RunWith(RobolectricTestRunner::class)
class QuestObjectiveScenarioTest {

    private lateinit var env: QuestTestEnvironment

    @Before
    fun setUp() {
        env = QuestTestEnvironment()
    }

    @After
    fun tearDown() {
        env.close()
    }

    // ------------------------------------------------------------------ order

    @Test
    fun objectives_ofAQuest_comeBackInOrderIndexOrder() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val third = objectivesIn(questId).last().id

        env.objectives.moveObjective(third, QuestObjectiveRepository.MoveDirection.UP)

        val objectives = objectivesIn(questId)
        assertEquals(listOf("First", "Third", "Second"), objectives.map { it.title })
        // The ids are now out of ascending order, which is what makes the previous line mean
        // something: the list follows the stored position and not the order the rows were written
        // in, so the DM's arrangement is what the screen shows after a reload.
        assertTrue(objectives[1].id > objectives[2].id)
        assertEquals(listOf(0, 1, 2), objectives.map { it.orderIndex })
    }

    @Test
    fun aNewObjective_landsAtTheEndOfTheList() = runTest {

        val questId = questWithObjectives("First", "Second")

        env.objectives.addObjective(questId = questId, title = "Third")

        val objectives = objectivesIn(questId)
        // A step written down later is the next one by default, so the DM types without having to
        // decide where it goes; moving it is one gesture away.
        assertEquals(listOf("First", "Second", "Third"), objectives.map { it.title })
        assertEquals(2, objectives.last().orderIndex)
    }

    @Test
    fun theFirstObjectiveOfAQuest_landsAtPositionZero() = runTest {

        val questId = questWithObjectives("First")

        // The quest had no rows to take a maximum from, and a list that started at 1 would collide
        // with the dense 0..n-1 run every later renumbering produces.
        assertEquals(0, objectivesIn(questId).single().orderIndex)
    }

    @Test
    fun moveObjective_up_swapsTheObjectiveWithTheOneAboveIt() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val second = objectivesIn(questId)[1].id

        env.objectives.moveObjective(second, QuestObjectiveRepository.MoveDirection.UP)

        val objectives = objectivesIn(questId)
        assertEquals(listOf("Second", "First", "Third"), objectives.map { it.title })
        // One swap and not a rotation: the step that was passed moves down by exactly one.
        assertEquals(listOf(0, 1, 2), objectives.map { it.orderIndex })
    }

    @Test
    fun moveObjective_down_swapsTheObjectiveWithTheOneBelowIt() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val second = objectivesIn(questId)[1].id

        env.objectives.moveObjective(second, QuestObjectiveRepository.MoveDirection.DOWN)

        val objectives = objectivesIn(questId)
        assertEquals(listOf("First", "Third", "Second"), objectives.map { it.title })
        assertEquals(listOf(0, 1, 2), objectives.map { it.orderIndex })
    }

    @Test
    fun movingTheFirstObjective_up_changesNothing() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val first = objectivesIn(questId).first().id

        env.objectives.moveObjective(first, QuestObjectiveRepository.MoveDirection.UP)

        // The screen disables the arrow, but the repository is what makes that true - a stale
        // screen tapping it must not wrap the step around to the bottom.
        assertEquals(listOf("First", "Second", "Third"), titlesIn(questId))
        assertEquals(listOf(0, 1, 2), objectivesIn(questId).map { it.orderIndex })
    }

    @Test
    fun movingTheLastObjective_down_changesNothing() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val last = objectivesIn(questId).last().id

        env.objectives.moveObjective(last, QuestObjectiveRepository.MoveDirection.DOWN)

        assertEquals(listOf("First", "Second", "Third"), titlesIn(questId))
        assertEquals(listOf(0, 1, 2), objectivesIn(questId).map { it.orderIndex })
    }

    @Test
    fun theOnlyObjectiveOfAQuest_cannotBeMovedEitherWay() = runTest {

        val questId = questWithObjectives("First")
        val only = objectivesIn(questId).single().id

        env.objectives.moveObjective(only, QuestObjectiveRepository.MoveDirection.UP)
        env.objectives.moveObjective(only, QuestObjectiveRepository.MoveDirection.DOWN)

        // Both ends at once: with one row the move is degenerate in both directions.
        assertEquals(listOf("First"), titlesIn(questId))
        assertEquals(0, objectivesIn(questId).single().orderIndex)
    }

    @Test
    fun deletingAnObjectiveFromTheMiddle_leavesTheRestInTheSameOrder() = runTest {

        val questId = questWithObjectives("First", "Second", "Third", "Fourth")
        val second = objectivesIn(questId)[1].id

        env.objectives.deleteObjective(second)

        // The deletion leaves a hole in the numbering, and the list is still right anyway: the
        // query orders by position, and a gap does not reorder what is left.
        assertEquals(listOf("First", "Third", "Fourth"), titlesIn(questId))
    }

    @Test
    fun aMove_afterADeletionFromTheMiddle_renumbersTheQuestToADenseRun() = runTest {

        val questId = questWithObjectives("First", "Second", "Third", "Fourth")
        val second = objectivesIn(questId)[1].id
        env.objectives.deleteObjective(second)
        val fourth = objectivesIn(questId).last().id

        env.objectives.moveObjective(fourth, QuestObjectiveRepository.MoveDirection.UP)

        val objectives = objectivesIn(questId)
        assertEquals(listOf("First", "Fourth", "Third"), objectives.map { it.title })
        // The move works off the actual row order and rewrites the whole quest in one transaction,
        // which is what heals the hole the deletion left. Nothing else renumbers, so this is the
        // only place the invariant "positions are a dense 0..n-1 run" is restored - and if it ever
        // stopped being restored, the holes would accumulate silently for as long as the list
        // happened to still read correctly.
        assertEquals(listOf(0, 1, 2), objectives.map { it.orderIndex })
    }

    // --------------------------------------------------------------- parallel

    @Test
    fun completingALaterObjective_whileTheEarlierOnesAreUntouched_isAccepted() = runTest {

        val questId = questWithObjectives("First", "Second", "Third")
        val third = objectivesIn(questId).last().id

        env.objectives.updateObjectiveStatus(third, QuestObjectiveStatus.COMPLETED)

        val objectives = objectivesIn(questId)
        // This is the whole of "parallel objectives": there is no prerequisite to satisfy, so three
        // steps the DM thinks of as open at the same time are ticked off in whatever order they
        // actually happen at the table. A gate here would turn every list into a sequence.
        assertEquals(QuestObjectiveStatus.COMPLETED, objectives[2].status)
        assertEquals(QuestObjectiveStatus.NOT_STARTED, objectives[0].status)
        assertEquals(QuestObjectiveStatus.NOT_STARTED, objectives[1].status)
        // And a status change is not a reorder: ticking a step does not move it.
        assertEquals(listOf("First", "Second", "Third"), objectives.map { it.title })
    }

    @Test
    fun reopeningACompletedObjective_isAccepted() = runTest {

        val questId = questWithObjectives("First")
        val only = objectivesIn(questId).single().id
        env.objectives.updateObjectiveStatus(only, QuestObjectiveStatus.COMPLETED)

        env.objectives.updateObjectiveStatus(only, QuestObjectiveStatus.NOT_STARTED)

        // Nothing is terminal: a step ticked by mistake has to be as easy to untick, and whether a
        // step still counts is a judgement about the story.
        assertEquals(QuestObjectiveStatus.NOT_STARTED, objectivesIn(questId).single().status)
    }

    // --------------------------------------------------------------- optional

    @Test
    fun addObjective_asOptional_storesAndReadsBackTheFlag() = runTest {

        val questId = questWithObjectives()

        env.objectives.addObjective(
            questId = questId,
            title = "Hear out Madam Eva",
            isOptional = true
        )

        assertTrue(objectivesIn(questId).single().isOptional)
    }

    @Test
    fun addObjective_withoutSayingOtherwise_storesARequiredObjective() = runTest {

        val questId = questWithObjectives()

        env.objectives.addObjective(questId = questId, title = "Reach the Tser Pool")

        // The default has to be the stricter of the two: a step silently marked optional is a step
        // the DM stops counting without ever deciding to.
        assertFalse(objectivesIn(questId).single().isOptional)
    }

    @Test
    fun updateObjective_flipsTheOptionalFlagBothWays() = runTest {

        val questId = questWithObjectives("First", "Second")
        val second = objectivesIn(questId)[1]

        env.objectives.updateObjective(
            id = second.id,
            title = second.title,
            description = second.description,
            isOptional = true
        )
        assertTrue(objectivesIn(questId)[1].isOptional)

        env.objectives.updateObjective(
            id = second.id,
            title = second.title,
            description = second.description,
            isOptional = false
        )
        // Both directions, because a step reclassified as required again is the ordinary correction
        // of a mistake and not a special case.
        assertFalse(objectivesIn(questId)[1].isOptional)
    }

    @Test
    fun updateObjective_leavesTheStatusAndThePositionAlone() = runTest {

        val questId = questWithObjectives("First", "Second")
        val second = objectivesIn(questId)[1]
        env.objectives.updateObjectiveStatus(second.id, QuestObjectiveStatus.COMPLETED)

        env.objectives.updateObjective(
            id = second.id,
            title = "Second, reworded",
            description = "and described",
            isOptional = true
        )

        val updated = objectivesIn(questId)[1]
        assertEquals("Second, reworded", updated.title)
        assertEquals("and described", updated.description)
        // The form writes three fields as a whole. If it ever wrote the status or the position too,
        // saving reworded text would undo a tick or a reorder made elsewhere on the screen.
        assertEquals(QuestObjectiveStatus.COMPLETED, updated.status)
        assertEquals(1, updated.orderIndex)
    }

    @Test
    fun anOptionalObjective_isListedAndOrderedLikeARequiredOne() = runTest {

        val questId = questWithObjectives("First")
        env.objective(questId = questId, title = "Second", isOptional = true)
        env.objective(questId = questId, title = "Third")
        val optional = objectivesIn(questId)[1].id

        env.objectives.moveObjective(optional, QuestObjectiveRepository.MoveDirection.DOWN)

        val objectives = objectivesIn(questId)
        // Marked rather than hidden, and not sorted to the bottom either: an optional step is still
        // part of the quest and sits where the DM put it. The distinction is meaning, not mechanics.
        assertEquals(listOf("First", "Third", "Second"), objectives.map { it.title })
        assertEquals(listOf(0, 1, 2), objectives.map { it.orderIndex })
        assertTrue(objectives.last().isOptional)
    }

    @Test
    fun anOptionalObjective_takesAStatusLikeARequiredOne() = runTest {

        val questId = questWithObjectives()
        val optional = env.objective(
            questId = questId,
            title = "Hear out Madam Eva",
            isOptional = true
        )

        env.objectives.updateObjectiveStatus(optional, QuestObjectiveStatus.COMPLETED)

        val stored = objectivesIn(questId).single()
        assertEquals(QuestObjectiveStatus.COMPLETED, stored.status)
        // The flag is not consumed by anything: it says what it said before the step was ticked.
        assertTrue(stored.isOptional)
    }

    // -------------------------------------------------------- no auto-finish

    @Test
    fun completingEveryObjective_ofAnActiveQuest_leavesTheQuestStatusAlone() = runTest {

        val questId = questWithObjectives("First", "Second")
        env.quests.updateQuestStatus(questId, QuestStatus.ACTIVE)

        objectivesIn(questId).forEach {
            env.objectives.updateObjectiveStatus(it.id, QuestObjectiveStatus.COMPLETED)
        }

        // The rule the specification asks for and the one thing in this module that cannot be
        // recovered if it slips: a quest whose status moved on its own has made a decision about
        // the story on the DM's behalf, and the DM has no way to tell it apart from their own note.
        val quest = env.questById(questId)
        assertNotNull(quest)
        assertEquals(QuestStatus.ACTIVE, quest?.status)
        assertTrue(objectivesIn(questId).all { it.status == QuestObjectiveStatus.COMPLETED })
        // The log is the second witness: exactly the one status change that was asked for by hand.
        assertEquals(
            1,
            historyFor(questId).count {
                it.eventType == QuestHistoryEventType.QUEST_STATUS_CHANGED
            }
        )
    }

    @Test
    fun completingEveryObjective_includingTheOptionalOnes_stillLeavesTheQuestAlone() = runTest {

        val questId = questWithObjectives("First")
        env.objective(questId = questId, title = "Second", isOptional = true)
        env.quests.updateQuestStatus(questId, QuestStatus.PAUSED)

        objectivesIn(questId).forEach {
            env.objectives.updateObjectiveStatus(it.id, QuestObjectiveStatus.COMPLETED)
        }

        // Asserted separately from the all-required case because "everything required is done" is
        // the tempting place to put an auto-transition, and an optional step left open is the only
        // thing that would have held it back there.
        assertEquals(QuestStatus.PAUSED, env.questById(questId)?.status)
    }

    @Test
    fun failingEveryObjective_leavesTheQuestStatusAlone() = runTest {

        val questId = questWithObjectives("First", "Second")

        objectivesIn(questId).forEach {
            env.objectives.updateObjectiveStatus(it.id, QuestObjectiveStatus.FAILED)
        }

        // The same rule from the other side. A party that failed every step may well have found
        // another way, and the quest is still the one the DM left at NOT_STARTED.
        assertEquals(QuestStatus.NOT_STARTED, env.questById(questId)?.status)
    }

    // ---------------------------------------------------------------- history

    @Test
    fun completingAnObjective_appendsOneEntryNamingIt() = runTest {

        val questId = questWithObjectives()
        val objective = env.objective(questId = questId, title = "Reach the Tser Pool")

        env.objectives.updateObjectiveStatus(objective, QuestObjectiveStatus.COMPLETED)

        val history = historyFor(questId)
        // Exactly one, because the entry is written in the same transaction as the status: a second
        // copy would make the log say the step was ticked twice.
        assertEquals(1, history.size)
        assertEquals(QuestHistoryEventType.OBJECTIVE_COMPLETED, history.single().eventType)
        // The title and not a finished sentence - the log is read in whatever language the app is
        // in when it is read, not the one it was written in.
        assertEquals("Reach the Tser Pool", history.single().description)
    }

    @Test
    fun failingAnObjective_isLoggedAsFailedAndNotAsCompleted() = runTest {

        val questId = questWithObjectives()
        val objective = env.objective(questId = questId, title = "Save the Vallakian burgomaster")

        env.objectives.updateObjectiveStatus(objective, QuestObjectiveStatus.FAILED)

        val entry = historyFor(questId).single()
        assertEquals(QuestHistoryEventType.OBJECTIVE_FAILED, entry.eventType)
        assertEquals("Save the Vallakian burgomaster", entry.description)
    }

    @Test
    fun untickingAnObjective_isLoggedAsReopened() = runTest {

        val questId = questWithObjectives()
        val objective = env.objective(questId = questId, title = "Reach the Tser Pool")
        env.objectives.updateObjectiveStatus(objective, QuestObjectiveStatus.COMPLETED)

        env.objectives.updateObjectiveStatus(objective, QuestObjectiveStatus.NOT_STARTED)

        val history = historyFor(questId)
        assertEquals(2, history.size)
        // Newest first, and "reopened" rather than "not started": the step did start once, and the
        // log is about what the DM did, not about where the step now stands.
        assertEquals(QuestHistoryEventType.OBJECTIVE_REOPENED, history.first().eventType)
        assertEquals(QuestHistoryEventType.OBJECTIVE_COMPLETED, history[1].eventType)
    }

    @Test
    fun theHistoryOfAQuest_doesNotShowAnotherQuestsObjectives() = runTest {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Save the miller's daughter")
        val otherQuestId = env.quest(campaignId, "Find the missing wine shipment")
        val objective = env.objective(questId = questId, title = "Reach the Tser Pool")

        env.objectives.updateObjectiveStatus(objective, QuestObjectiveStatus.COMPLETED)

        // The entry is filed against the quest read from the objective's own row, so a step can
        // only ever write into the log of the quest it belongs to.
        assertEquals(1, historyFor(questId).size)
        assertTrue(historyFor(otherQuestId).isEmpty())
    }

    // -------------------------------------------------------------- shorthand

    /**
     * A fresh quest in a fresh campaign, with [titles] added as objectives in the order given.
     *
     * Every test here needs that same three-line preamble, and naming the campaign and the quest
     * would only add words to assertions that are about neither.
     */
    private suspend fun questWithObjectives(vararg titles: String): Int {

        val campaignId = env.campaign()
        val questId = env.quest(campaignId, "Save the miller's daughter")

        titles.forEach {
            env.objective(questId = questId, title = it)
        }

        return questId
    }

    private suspend fun objectivesIn(questId: Int): List<QuestObjective> =
        env.objectives.getObjectivesForQuest(questId).first()

    private suspend fun titlesIn(questId: Int): List<String> =
        objectivesIn(questId).map { it.title }

    private suspend fun historyFor(questId: Int): List<QuestHistoryEntry> =
        env.history.getHistoryForQuest(questId).first()
}
