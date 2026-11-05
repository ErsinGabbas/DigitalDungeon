package com.example.digitaldungeon.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.CampaignWithParticipants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Verifies how [AppDatabase] behaves when it is opened on top of a database file whose
 * version has no migration path (1 and 2 were never exported).
 *
 * The database is always built with exactly the configuration the Hilt provider uses
 * ([ALL_MIGRATIONS] + [DESTRUCTIVELY_MIGRATED_VERSIONS]), only the file name differs, so
 * these tests break if that configuration is changed in `di/AppModule.kt`.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseFallbackTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        database?.takeIf { it.isOpen }?.close()
        database = null
        context.deleteDatabase(TEST_DB)
    }

    /**
     * A version 2 database has no migration path, so opening it must not throw:
     * [DESTRUCTIVELY_MIGRATED_VERSIONS] makes Room drop it and recreate the current schema.
     */
    @Test
    fun databaseAtVersion2_isRecreatedInsteadOfCrashing() {
        createLegacyDatabaseAtVersion2()

        // Sanity check: the file we handed to Room really is a version 2 database.
        assertEquals(LEGACY_VERSION, readUserVersionFromFile())

        val db = buildDatabase()
        // Touching the open helper is what actually opens the file and triggers the fallback.
        val openedVersion = db.openHelper.writableDatabase.version

        assertEquals(CURRENT_VERSION, openedVersion)
        assertTrue(
            "Recreated database must contain the current schema",
            tableNames(db).containsAll(EXPECTED_TABLES)
        )

        // Destructive recreation is the documented trade-off: legacy rows are gone,
        // and the recreated `campaigns` table carries the current columns.
        val campaigns = runBlocking { db.campaignDao().getCampaigns().first() }
        assertEquals(
            "Rows from the version 2 database must not survive a destructive migration",
            emptyList<CampaignWithParticipants>(),
            campaigns
        )
        assertTrue(
            "Recreated campaigns table must not have the pre-version-8 `participants` column",
            !columnNames(db, "campaigns").contains("participants")
        )
        assertTrue(
            "Recreated schema must carry the version 8 `campaign_participants` columns",
            columnNames(db, "campaign_participants").containsAll(
                setOf("id", "campaignId", "name", "level", "maxHp", "orderIndex")
            )
        )
    }

    /**
     * Without any pre-existing file the same builder has to create the current schema outright.
     */
    @Test
    fun freshDatabase_isCreatedAtCurrentVersionWithAllTables() {
        assertTrue(
            "Precondition: no database file must exist",
            !context.getDatabasePath(TEST_DB).exists()
        )

        val db = buildDatabase()

        assertEquals(CURRENT_VERSION, db.openHelper.writableDatabase.version)
        assertEquals(EXPECTED_TABLES, tableNames(db).intersect(EXPECTED_TABLES))
    }

    /**
     * End-to-end smoke test over entity -> DAO on a freshly created database, including the
     * `campaign_participants` relation that replaced the participants JSON column in version 8.
     */
    @Test
    fun freshDatabase_supportsInsertAndReadThroughDao() {
        val db = buildDatabase()
        val dao = db.campaignDao()
        val participantDao = db.campaignParticipantDao()

        val inserted = CampaignEntity(
            name = "Curse of Strahd",
            description = "Gothic horror in Barovia",
            createdAt = 1_700_000_000_000L
        )

        val fromFlow = runBlocking {
            val campaignId = dao.insertCampaign(inserted).toInt()
            participantDao.insertParticipants(
                listOf(
                    CampaignParticipantEntity(
                        campaignId = campaignId,
                        name = "Ireena",
                        level = 3,
                        maxHp = 24,
                        orderIndex = 0
                    ),
                    CampaignParticipantEntity(
                        campaignId = campaignId,
                        name = "Ismark",
                        level = 4,
                        maxHp = 31,
                        orderIndex = 1
                    )
                )
            )
            dao.getCampaigns().first()
        }

        assertEquals(1, fromFlow.size)
        val stored = fromFlow.single()
        assertTrue("Primary key must be generated by Room", stored.campaign.id > 0)
        assertEquals(inserted.name, stored.campaign.name)
        assertEquals(inserted.description, stored.campaign.description)
        assertEquals(inserted.createdAt, stored.campaign.createdAt)
        assertEquals(
            listOf("Ireena", "Ismark"),
            stored.participants.sortedBy { it.orderIndex }.map { it.name }
        )

        val byId = runBlocking { dao.getCampaignById(stored.campaign.id) }
        assertNotNull("Campaign must be readable by its generated id", byId)
        assertEquals(stored.campaign, byId!!.campaign)
        assertEquals(
            stored.participants.sortedBy { it.orderIndex },
            byId.participants.sortedBy { it.orderIndex }
        )
    }

    /**
     * Builds the database exactly like `AppModule.provideDatabase`, but against [TEST_DB].
     */
    private fun buildDatabase(): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationFrom(*DESTRUCTIVELY_MIGRATED_VERSIONS)
            .build()
            .also { database = it }

    /**
     * Writes a plausible pre-migration database by hand: version 1 and 2 schemas were never
     * exported, so `MigrationTestHelper` cannot produce them.
     */
    private fun createLegacyDatabaseAtVersion2() {
        val file = databaseFile()
        file.parentFile?.mkdirs()

        val legacy = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            legacy.execSQL(
                """
                CREATE TABLE campaigns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            legacy.execSQL(
                "INSERT INTO campaigns (name, description, createdAt) VALUES (?, ?, ?)",
                arrayOf<Any>("Legacy campaign", "Created before version 3", 1L)
            )
            // Room reads the file version from PRAGMA user_version.
            legacy.execSQL("PRAGMA user_version = $LEGACY_VERSION")
        } finally {
            legacy.close()
        }
    }

    private fun databaseFile(): File = context.getDatabasePath(TEST_DB)

    private fun readUserVersionFromFile(): Int {
        val db = SQLiteDatabase.openDatabase(
            databaseFile().absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        return try {
            db.version
        } finally {
            db.close()
        }
    }

    private fun tableNames(db: AppDatabase): Set<String> {
        val names = mutableSetOf<String>()
        db.openHelper.writableDatabase
            .query("SELECT name FROM sqlite_master WHERE type = 'table'")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    names += cursor.getString(0)
                }
            }
        return names
    }

    private fun columnNames(db: AppDatabase, table: String): Set<String> {
        val names = mutableSetOf<String>()
        db.openHelper.writableDatabase
            .query("PRAGMA table_info($table)")
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    names += cursor.getString(nameIndex)
                }
            }
        return names
    }

    private companion object {
        /** Deliberately different from the production `digital_dungeon_db`. */
        const val TEST_DB = "fallback_test_db"
        const val LEGACY_VERSION = 2
        const val CURRENT_VERSION = 8
        val EXPECTED_TABLES =
            setOf("campaigns", "campaign_participants", "sessions", "session_events")
    }
}
