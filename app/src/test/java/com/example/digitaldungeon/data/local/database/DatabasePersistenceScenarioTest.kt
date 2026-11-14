package com.example.digitaldungeon.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.digitaldungeon.data.repository.QuestRepositoryImpl
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Scenarios 12 and 13: migrating an old database, and surviving a restart.
 *
 * These are the only tests in the JVM suite that use a database **file** rather than an in-memory
 * one, because both scenarios are about what happens between two openings of the same database -
 * which an in-memory database cannot have, since closing it destroys it.
 *
 * Scenario 12 picks up where the instrumented `MigrationTest` stops. That one covers 4 -> 10 with
 * `MigrationTestHelper`, which needs the exported schemas from an APK's assets and therefore a
 * device. The three migrations added since (10 -> 11, 11 -> 12, 12 -> 13) had no coverage at all, so
 * this test builds a version 9 database by hand and walks it all the way up, asserting both halves
 * of what a migration has to get right: the old rows are still there, and the new tables work.
 *
 * The version 9 schema below is a verbatim copy of `app/schemas/.../9.json`. Copied rather than read
 * from disk because a unit test's working directory is not something to build a schema on, and
 * because a divergence here would be loud: if these statements stopped matching what version 9
 * actually was, Room's own validator would refuse the migrated database at the end.
 */
@RunWith(RobolectricTestRunner::class)
class DatabasePersistenceScenarioTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private var database: AppDatabase? = null

    @Before
    fun removeLeftoverDatabase() {
        context.deleteDatabase(DB_NAME)
    }

    @After
    fun removeTestDatabase() {
        database?.close()
        database = null
        context.deleteDatabase(DB_NAME)
    }

    /**
     * Opens the database file through Room, exactly as the app's Hilt provider does.
     *
     * No `fallbackToDestructiveMigration`, on purpose: without it, a missing or wrong migration is
     * an exception instead of a silently wiped journal, which is the behaviour these tests are for.
     */
    private fun openWithRoom(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { database = it }

    /** Creates the database file at version 9 and hands it to [fill] before closing it again. */
    private fun createVersion9Database(fill: SupportSQLiteDatabase.() -> Unit) {

        val configuration = SupportSQLiteOpenHelper.Configuration
            .builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {

                override fun onCreate(db: SupportSQLiteDatabase) {
                    VERSION_9_SCHEMA.forEach(db::execSQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.use { db ->
            // Foreign keys are off by default on a raw connection, and the rows below are inserted
            // parent-first anyway; turning them on keeps the fixture honest about what it writes.
            db.execSQL("PRAGMA foreign_keys = ON")
            db.fill()
        }
        helper.close()
    }

    private fun SupportSQLiteDatabase.insertLegacyFixture() {

        execSQL(
            "INSERT INTO campaigns (id, name, description, createdAt) " +
                "VALUES (1, 'Curse of Strahd', 'Barovia', 100)"
        )
        execSQL(
            "INSERT INTO campaign_participants (id, campaignId, name, level, maxHp, orderIndex) " +
                "VALUES (1, 1, 'Ismark', 5, 42, 0)"
        )
        execSQL(
            "INSERT INTO sessions (id, campaignId, title, notes, createdAt) " +
                "VALUES (1, 1, 'Session 1', 'The village', 200)"
        )
        execSQL(
            "INSERT INTO session_events " +
                "(id, sessionId, type, title, content, orderIndex, timestamp, participants) " +
                "VALUES (1, 1, 'SCENE', 'Arrival', 'The mists part', 0, 300, NULL)"
        )
        execSQL(
            "INSERT INTO quests " +
                "(id, campaignId, title, description, category, status, priority, isHidden, " +
                "createdAt, inGameDate) " +
                "VALUES (1, 1, 'The missing daughter', 'Ask the miller', 'SIDE', 'ACTIVE', " +
                "'HIGH', 0, 400, 'Neufarbenfest')"
        )
    }

    // ------------------------------------------------- scenario 12: migrating an old database

    @Test
    fun aVersion9Database_opensAtTheCurrentVersion_throughEveryMigrationInBetween() = runTest {

        createVersion9Database { insertLegacyFixture() }

        val db = openWithRoom()

        // Opening is the assertion: Room validates the migrated schema against the entities and
        // throws if a single column, index or foreign key disagrees.
        assertEquals(13, db.openHelper.writableDatabase.version)
    }

    @Test
    fun aVersion9Database_keepsEveryRowItHad() = runTest {

        createVersion9Database { insertLegacyFixture() }

        val db = openWithRoom()

        assertEquals(
            listOf("Curse of Strahd"),
            db.campaignDao().getAllCampaigns().map { it.name }
        )
        assertEquals(
            listOf("Ismark"),
            db.campaignParticipantDao().getAllParticipants().map { it.name }
        )
        assertEquals(listOf("Session 1"), db.sessionDao().getAllSessions().map { it.title })
        assertEquals(listOf("Arrival"), db.sessionEventDao().getAllEvents().map { it.title })

        // The quest keeps every field, enums included: they are stored as TEXT by constant name,
        // so a renamed constant would show up here as a crash rather than as silent data loss.
        val quest = db.questDao().getAllQuests().single()
        assertEquals("The missing daughter", quest.title)
        assertEquals("Ask the miller", quest.description)
        assertEquals(QuestCategory.SIDE, quest.category)
        assertEquals(QuestStatus.ACTIVE, quest.status)
        assertEquals(QuestPriority.HIGH, quest.priority)
        assertFalse(quest.isHidden)
        assertEquals(400L, quest.createdAt)
        assertEquals("Neufarbenfest", quest.inGameDate)
    }

    @Test
    fun aVersion9Database_gainsTheTablesAddedSince_andTheyTakeRows() = runTest {

        createVersion9Database { insertLegacyFixture() }

        val db = openWithRoom()

        // Empty, because version 9 had nowhere to keep any of this. The point of the assertion is
        // that the queries run at all: a migration that forgot a table or an index would fail here.
        assertTrue(db.questObjectiveDao().getAllObjectives().isEmpty())
        assertTrue(db.questRewardDao().getAllRewards().isEmpty())
        assertTrue(db.questConsequenceDao().getAllConsequences().isEmpty())
        assertTrue(db.questNoteDao().getAllNotes().isEmpty())
        assertTrue(db.questRelationDao().getAllRelations().isEmpty())
        assertTrue(db.questHistoryDao().getAllEntries().isEmpty())
        assertTrue(db.questSessionLinkDao().getAllLinks().isEmpty())

        // And that they are writable, which is the part a CREATE TABLE alone does not prove: the
        // foreign key into the migrated `quests` row has to resolve.
        val repository = QuestRepositoryImpl(
            database = db,
            dao = db.questDao(),
            historyDao = db.questHistoryDao()
        )
        repository.updateQuestStatus(id = 1, status = QuestStatus.COMPLETED)

        assertEquals(
            listOf(QuestStatus.COMPLETED.name),
            db.questHistoryDao().getAllEntries().map { it.description }
        )
    }

    @Test
    fun aVersion9DatabaseWithNoQuests_migratesJustTheSame() = runTest {

        createVersion9Database {
            execSQL(
                "INSERT INTO campaigns (id, name, description, createdAt) " +
                    "VALUES (1, 'Lost Mine of Phandelver', '', 100)"
            )
        }

        val db = openWithRoom()

        assertEquals(1, db.campaignDao().getAllCampaigns().size)
        assertTrue(db.questDao().getAllQuests().isEmpty())
        assertEquals(13, db.openHelper.writableDatabase.version)
    }

    // ------------------------------------------------- scenario 13: surviving a restart

    @Test
    fun everythingWrittenToAQuest_isStillThereAfterReopeningTheDatabase() = runTest {

        // First run of the app: write a quest with one of everything attached.
        val firstRun = openWithRoom()
        val firstQuests = QuestRepositoryImpl(
            database = firstRun,
            dao = firstRun.questDao(),
            historyDao = firstRun.questHistoryDao()
        )

        firstRun.campaignDao().insertCampaign(
            com.example.digitaldungeon.data.local.entity.CampaignEntity(
                id = 1,
                name = "Curse of Strahd",
                description = "",
                createdAt = 100
            )
        )
        firstQuests.addQuest(
            campaignId = 1,
            title = "The missing daughter",
            description = "Ask the miller",
            category = QuestCategory.MAIN,
            status = QuestStatus.ACTIVE,
            priority = QuestPriority.HIGH,
            isHidden = true,
            inGameDate = "Neufarbenfest"
        )
        val questId = firstQuests.getQuestsForCampaign(1).first().single().id
        firstQuests.updateQuestStatus(questId, QuestStatus.COMPLETED)

        firstRun.close()
        database = null

        // Second run: a new Room instance over the same file, which is what a restart is.
        val secondRun = openWithRoom()

        val quest = secondRun.questDao().getAllQuests().single()
        assertEquals("The missing daughter", quest.title)
        assertEquals("Ask the miller", quest.description)
        assertEquals(QuestCategory.MAIN, quest.category)
        assertEquals(QuestStatus.COMPLETED, quest.status)
        assertEquals(QuestPriority.HIGH, quest.priority)
        assertTrue(quest.isHidden)
        assertEquals("Neufarbenfest", quest.inGameDate)

        // The history survives too, and that matters more than the quest: an append-only log that
        // did not outlive the process would be a log of the current session, not of the campaign.
        assertEquals(
            listOf(QuestStatus.COMPLETED.name),
            secondRun.questHistoryDao().getAllEntries().map { it.description }
        )
    }

    @Test
    fun aQuestDeletedBeforeTheRestart_isStillGoneAfterIt() = runTest {

        val firstRun = openWithRoom()
        val firstQuests = QuestRepositoryImpl(
            database = firstRun,
            dao = firstRun.questDao(),
            historyDao = firstRun.questHistoryDao()
        )

        firstRun.campaignDao().insertCampaign(
            com.example.digitaldungeon.data.local.entity.CampaignEntity(
                id = 1,
                name = "Curse of Strahd",
                description = "",
                createdAt = 100
            )
        )
        firstQuests.addQuest(campaignId = 1, title = "Kept")
        firstQuests.addQuest(campaignId = 1, title = "Deleted")

        val deletedId = firstQuests.getQuestsForCampaign(1).first()
            .single { it.title == "Deleted" }.id
        firstQuests.deleteQuest(deletedId)

        firstRun.close()
        database = null

        val secondRun = openWithRoom()

        // A delete that only held in memory would be the worst of the two failures: the user would
        // see the quest return without ever being told it had.
        assertEquals(listOf("Kept"), secondRun.questDao().getAllQuests().map { it.title })
    }

    private companion object {

        const val DB_NAME = "persistence-scenario.db"

        /**
         * The schema of version 9, verbatim from the exported JSON.
         *
         * Indices included: Room validates them on open, so a fixture missing one would fail the
         * migration for a reason that has nothing to do with the migration.
         */
        val VERSION_9_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `campaigns` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",

            "CREATE TABLE IF NOT EXISTS `campaign_participants` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`campaignId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                "`level` INTEGER NOT NULL, `maxHp` INTEGER NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "FOREIGN KEY(`campaignId`) REFERENCES `campaigns`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_campaign_participants_campaignId` " +
                "ON `campaign_participants` (`campaignId`)",

            "CREATE TABLE IF NOT EXISTS `sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`campaignId` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                "`notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`campaignId`) REFERENCES `campaigns`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_sessions_campaignId` " +
                "ON `sessions` (`campaignId`)",

            "CREATE TABLE IF NOT EXISTS `session_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, `type` TEXT NOT NULL, `title` TEXT, " +
                "`content` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `participants` TEXT, " +
                "FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_session_events_sessionId` " +
                "ON `session_events` (`sessionId`)",
            "CREATE INDEX IF NOT EXISTS `index_session_events_sessionId_orderIndex` " +
                "ON `session_events` (`sessionId`, `orderIndex`)",

            "CREATE TABLE IF NOT EXISTS `quests` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`campaignId` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `priority` TEXT NOT NULL, " +
                "`isHidden` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`inGameDate` TEXT, " +
                "FOREIGN KEY(`campaignId`) REFERENCES `campaigns`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_quests_campaignId` ON `quests` (`campaignId`)"
        )
    }
}
