package com.example.digitaldungeon.data.local.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [QuestObjectiveDao] against a real Room instance.
 *
 * The reordering is the reason this test exists. `moveObjective` renumbers a whole quest inside one
 * transaction, works off row order rather than arithmetic, and is expected to heal the gaps that
 * deleted steps leave behind - none of which can be checked against a fake. The cascade is the
 * other reason: an objective has to disappear both when its quest does and when the campaign two
 * levels up does.
 */
@RunWith(AndroidJUnit4::class)
class QuestObjectiveDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: QuestObjectiveDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // In-memory, so the test can never disturb the installed app's real journal.
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        dao = database.questObjectiveDao()

        runBlocking {
            database.campaignDao().insertCampaigns(
                listOf(
                    CampaignEntity(id = CAMPAIGN, name = "Barovia", description = "", createdAt = 1L)
                )
            )
            database.questDao().insertQuests(
                listOf(
                    quest(id = QUEST_A, title = "Escort Ireena"),
                    quest(id = QUEST_B, title = "Find the amber shard")
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------------- writing

    @Test
    fun insertObjective_withTheDefaultsTheRepositoryUses_isStored() = runBlocking {
        dao.insertObjective(
            QuestObjectiveEntity(
                questId = QUEST_A,
                title = "Reach the Tser Pool",
                description = "",
                status = QuestObjectiveStatus.NOT_STARTED,
                orderIndex = 0,
                isOptional = false
            )
        )

        val stored = dao.getObjectivesForQuest(QUEST_A).first().single()
        assertEquals("Reach the Tser Pool", stored.title)
        assertEquals("", stored.description)
        assertEquals(QuestObjectiveStatus.NOT_STARTED, stored.status)
        assertEquals(0, stored.orderIndex)
        assertEquals(false, stored.isOptional)
        assertTrue(stored.id > 0)
    }

    @Test
    fun insertObjective_forAQuestThatDoesNotExist_isRefused() = runBlocking {
        // The foreign key is what makes "every step belongs to a quest" a property of the schema.
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.insertObjective(objective(id = 1, questId = 999))
            }
        }
        Unit
    }

    @Test
    fun getMaxOrderIndex_isNullForAQuestWithoutObjectives_andTheLastPositionOtherwise() =
        runBlocking {
            assertNull(dao.getMaxOrderIndex(QUEST_A))

            dao.insertObjectives(
                listOf(
                    objective(id = 1, orderIndex = 0),
                    objective(id = 2, orderIndex = 1),
                    objective(id = 3, orderIndex = 2)
                )
            )

            // Which is what the repository adds one to when appending.
            assertEquals(2, dao.getMaxOrderIndex(QUEST_A))
            assertNull(dao.getMaxOrderIndex(QUEST_B))
        }

    // ----------------------------------------------------------- isolation

    @Test
    fun getObjectivesForQuest_neverReturnsAnotherQuestsSteps() = runBlocking {
        dao.insertObjectives(
            listOf(
                objective(id = 1, questId = QUEST_A, title = "A first", orderIndex = 0),
                objective(id = 2, questId = QUEST_A, title = "A second", orderIndex = 1),
                objective(id = 3, questId = QUEST_B, title = "B first", orderIndex = 0)
            )
        )

        assertEquals(
            listOf("A first", "A second"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
        assertEquals(
            listOf("B first"),
            dao.getObjectivesForQuest(QUEST_B).first().map { it.title }
        )
    }

    // --------------------------------------------------------------- order

    @Test
    fun getObjectivesForQuest_ordersByPositionAndNotByInsertionOrId() = runBlocking {
        dao.insertObjectives(
            listOf(
                objective(id = 1, title = "Third", orderIndex = 2),
                objective(id = 2, title = "First", orderIndex = 0),
                objective(id = 3, title = "Second", orderIndex = 1)
            )
        )

        assertEquals(
            listOf("First", "Second", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun moveObjective_up_swapsItWithTheOneAbove() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 2, moveUp = true)

        assertEquals(
            listOf("Second", "First", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun moveObjective_down_swapsItWithTheOneBelow() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 2, moveUp = false)

        assertEquals(
            listOf("First", "Third", "Second"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun moveObjective_leavesADensePositionSequence() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 3, moveUp = true)

        // 0..n-1 with no gaps and no duplicates, which is what the next append counts on.
        assertEquals(
            listOf(0, 1, 2),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.orderIndex }
        )
    }

    @Test
    fun moveObjective_healsTheGapsLeftBehindByADeletedStep() = runBlocking {
        // Positions 0, 5 and 9 - what a list looks like after a few deletions.
        dao.insertObjectives(
            listOf(
                objective(id = 1, title = "First", orderIndex = 0),
                objective(id = 2, title = "Second", orderIndex = 5),
                objective(id = 3, title = "Third", orderIndex = 9)
            )
        )

        dao.moveObjective(objectiveId = 3, moveUp = true)

        val stored = dao.getObjectivesForQuest(QUEST_A).first()
        assertEquals(listOf("First", "Third", "Second"), stored.map { it.title })
        // The whole quest is renumbered, which is why arithmetic on the old indices was never
        // needed in the first place.
        assertEquals(listOf(0, 1, 2), stored.map { it.orderIndex })
    }

    @Test
    fun movingTheFirstObjectiveUp_changesNothing() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 1, moveUp = true)

        assertEquals(
            listOf("First", "Second", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun movingTheLastObjectiveDown_changesNothing() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 3, moveUp = false)

        assertEquals(
            listOf("First", "Second", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun movingAnUnknownObjective_changesNothing() = runBlocking {
        seedThree()

        dao.moveObjective(objectiveId = 99, moveUp = true)

        assertEquals(
            listOf("First", "Second", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun moveObjective_doesNotDisturbAnotherQuestsOrder() = runBlocking {
        seedThree()
        dao.insertObjectives(
            listOf(
                objective(id = 10, questId = QUEST_B, title = "B first", orderIndex = 0),
                objective(id = 11, questId = QUEST_B, title = "B second", orderIndex = 1)
            )
        )

        dao.moveObjective(objectiveId = 2, moveUp = true)

        assertEquals(
            listOf("B first", "B second"),
            dao.getObjectivesForQuest(QUEST_B).first().map { it.title }
        )
    }

    // ------------------------------------------------------------ updating

    @Test
    fun updateObjective_rewritesTheTextAndTheFlagAndNothingElse() = runBlocking {
        dao.insertObjective(
            objective(
                id = 1,
                title = "Reach the pool",
                description = "before dusk",
                status = QuestObjectiveStatus.COMPLETED,
                orderIndex = 3,
                isOptional = false
            )
        )

        dao.updateObjective(
            id = 1,
            title = "Reach the Tser Pool",
            description = "before nightfall",
            isOptional = true
        )

        val stored = dao.getObjectivesForQuest(QUEST_A).first().single()
        assertEquals("Reach the Tser Pool", stored.title)
        assertEquals("before nightfall", stored.description)
        assertEquals(true, stored.isOptional)
        // Neither is the form's to write: both are changed by their own gesture.
        assertEquals(QuestObjectiveStatus.COMPLETED, stored.status)
        assertEquals(3, stored.orderIndex)
    }

    @Test
    fun updateObjectiveStatus_touchesNothingButTheStatus() = runBlocking {
        dao.insertObjective(
            objective(
                id = 1,
                title = "Reach the pool",
                description = "before dusk",
                orderIndex = 2,
                isOptional = true
            )
        )

        dao.updateObjectiveStatus(1, QuestObjectiveStatus.COMPLETED)

        assertEquals(
            objective(
                id = 1,
                title = "Reach the pool",
                description = "before dusk",
                status = QuestObjectiveStatus.COMPLETED,
                orderIndex = 2,
                isOptional = true
            ),
            dao.getObjectivesForQuest(QUEST_A).first().single()
        )
    }

    @Test
    fun updateObjectiveStatus_goesBothWays() = runBlocking {
        dao.insertObjective(objective(id = 1))

        dao.updateObjectiveStatus(1, QuestObjectiveStatus.COMPLETED)
        assertEquals(
            QuestObjectiveStatus.COMPLETED,
            dao.getObjectivesForQuest(QUEST_A).first().single().status
        )

        // A step ticked off by mistake has to be as easy to untick.
        dao.updateObjectiveStatus(1, QuestObjectiveStatus.NOT_STARTED)
        assertEquals(
            QuestObjectiveStatus.NOT_STARTED,
            dao.getObjectivesForQuest(QUEST_A).first().single().status
        )

        dao.updateObjectiveStatus(1, QuestObjectiveStatus.FAILED)
        assertEquals(
            QuestObjectiveStatus.FAILED,
            dao.getObjectivesForQuest(QUEST_A).first().single().status
        )
    }

    // ------------------------------------------------------------ deleting

    @Test
    fun deleteObjective_removesOnlyThatStep() = runBlocking {
        seedThree()

        dao.deleteObjective(2)

        assertEquals(
            listOf("First", "Third"),
            dao.getObjectivesForQuest(QUEST_A).first().map { it.title }
        )
    }

    @Test
    fun deletingAQuest_takesItsObjectivesWithIt() = runBlocking {
        seedThree()
        dao.insertObjective(objective(id = 10, questId = QUEST_B, title = "B first"))

        database.questDao().deleteQuest(QUEST_A)

        assertTrue(dao.getObjectivesForQuest(QUEST_A).first().isEmpty())
        assertEquals(listOf(10), dao.getObjectivesForQuest(QUEST_B).first().map { it.id })
    }

    @Test
    fun deletingTheCampaign_reachesTheObjectivesTwoLevelsDown() = runBlocking {
        seedThree()

        database.campaignDao().deleteCampaign(CAMPAIGN)

        // campaigns -> quests -> quest_objectives, all three by CASCADE.
        assertTrue(database.questDao().getAllQuests().isEmpty())
        assertTrue(dao.getAllObjectives().isEmpty())
    }

    @Test
    fun deleteAllObjectives_emptiesTheTableWithoutTouchingTheQuests() = runBlocking {
        seedThree()

        dao.deleteAllObjectives()

        assertTrue(dao.getAllObjectives().isEmpty())
        // This is the restore path, not a cascade.
        assertEquals(2, database.questDao().getAllQuests().size)
    }

    @Test
    fun getAllObjectives_isOrderedForAStableBackupDocument() = runBlocking {
        dao.insertObjectives(
            listOf(
                objective(id = 3, questId = QUEST_B, orderIndex = 0),
                objective(id = 1, questId = QUEST_A, orderIndex = 1),
                objective(id = 2, questId = QUEST_A, orderIndex = 0)
            )
        )

        assertEquals(listOf(2, 1, 3), dao.getAllObjectives().map { it.id })
    }

    // ------------------------------------------------------------- helpers

    private suspend fun seedThree() {
        dao.insertObjectives(
            listOf(
                objective(id = 1, title = "First", orderIndex = 0),
                objective(id = 2, title = "Second", orderIndex = 1),
                objective(id = 3, title = "Third", orderIndex = 2)
            )
        )
    }

    private fun quest(id: Int, title: String) = QuestEntity(
        id = id,
        campaignId = CAMPAIGN,
        title = title,
        description = "",
        category = QuestCategory.SIDE,
        status = QuestStatus.NOT_STARTED,
        priority = QuestPriority.NORMAL,
        isHidden = false,
        createdAt = id * 10L
    )

    private fun objective(
        id: Int = 0,
        questId: Int = QUEST_A,
        title: String = "Objective $id",
        description: String = "",
        status: QuestObjectiveStatus = QuestObjectiveStatus.NOT_STARTED,
        orderIndex: Int = 0,
        isOptional: Boolean = false
    ) = QuestObjectiveEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        status = status,
        orderIndex = orderIndex,
        isOptional = isOptional
    )

    private companion object {
        const val CAMPAIGN = 1
        const val QUEST_A = 1
        const val QUEST_B = 2
    }
}
