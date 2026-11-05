package com.example.digitaldungeon.data.local.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration tests for [AppDatabase].
 *
 * Schemas for versions 1-3 were never exported, so only paths starting at version 4 can be tested.
 * Every test runs the app's real [ALL_MIGRATIONS] array: adding a migration to the app without
 * registering it there makes these tests fail.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Before
    fun removeLeftoverDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun removeTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate4To5_createsSessionEventsTable_andKeepsCampaignsAndSessions() {
        helper.createDatabase(TEST_DB, 4).apply {
            insertCampaign()
            insertSession()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, *ALL_MIGRATIONS)

        db.assertCampaignRowIntact(expectParticipantsColumn = false)
        db.assertSessionRowIntact()
        assertTrue("session_events table was not created", db.hasTable("session_events"))

        // The new table has to be usable, including its foreign key to the surviving session.
        db.insertSessionEvent()
        db.query("SELECT COUNT(*) FROM session_events").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate5To6_keepsExistingEvents_andAddsNullParticipants() {
        helper.createDatabase(TEST_DB, 5).apply {
            insertCampaign()
            insertSession()
            insertSessionEvent()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, *ALL_MIGRATIONS)

        db.query("SELECT * FROM session_events").use { cursor ->
            assertEquals("event row did not survive 5 -> 6", 1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(EVENT_ID, cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            assertEquals(SESSION_ID, cursor.getInt(cursor.getColumnIndexOrThrow("sessionId")))
            assertEquals(EVENT_TYPE, cursor.getString(cursor.getColumnIndexOrThrow("type")))
            assertEquals(EVENT_TITLE, cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(EVENT_CONTENT, cursor.getString(cursor.getColumnIndexOrThrow("content")))
            assertEquals(EVENT_ORDER_INDEX, cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex")))
            assertEquals(EVENT_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
            assertTrue(
                "participants must be NULL for rows that existed before the migration",
                cursor.isNull(cursor.getColumnIndexOrThrow("participants"))
            )
        }
        db.close()
    }

    @Test
    fun migrate6To7_keepsExistingCampaigns_andAddsNullParticipants() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertCampaign()
            insertSession()
            // session_events.participants already exists in v6, so it can carry a value across 6 -> 7.
            execSQL(
                """
                INSERT INTO session_events
                    (id, sessionId, type, title, content, orderIndex, timestamp, participants)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    EVENT_ID,
                    SESSION_ID,
                    EVENT_TYPE,
                    EVENT_TITLE,
                    EVENT_CONTENT,
                    EVENT_ORDER_INDEX,
                    EVENT_TIMESTAMP,
                    EVENT_PARTICIPANTS
                )
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, *ALL_MIGRATIONS)

        db.query("SELECT * FROM campaigns").use { cursor ->
            assertEquals("campaign row did not survive 6 -> 7", 1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(CAMPAIGN_ID, cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            assertEquals(CAMPAIGN_NAME, cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(
                CAMPAIGN_DESCRIPTION,
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            )
            assertEquals(CAMPAIGN_CREATED_AT, cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
            assertTrue(
                "participants must be NULL for rows that existed before the migration",
                cursor.isNull(cursor.getColumnIndexOrThrow("participants"))
            )
        }

        // Touching campaigns must not disturb data in the other tables.
        db.assertSessionRowIntact()
        db.query("SELECT participants FROM session_events WHERE id = $EVENT_ID").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(EVENT_PARTICIPANTS, cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate7To8_movesCampaignParticipantsJsonIntoRows() {
        helper.createDatabase(TEST_DB, 7).apply {
            insertLegacyCampaign(participantsJson = CAMPAIGN_PARTICIPANTS_JSON)
            insertSession()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, *ALL_MIGRATIONS)

        assertTrue(
            "campaign_participants table was not created",
            db.hasTable("campaign_participants")
        )

        // The whole point of the migration: the JSON roster has to exist as queryable rows,
        // with the list position preserved in orderIndex.
        db.query(
            "SELECT campaignId, name, level, maxHp, orderIndex " +
                "FROM campaign_participants ORDER BY orderIndex ASC"
        ).use { cursor ->
            assertEquals("both JSON participants must become rows", 2, cursor.count)

            assertTrue(cursor.moveToFirst())
            assertEquals(CAMPAIGN_ID, cursor.getInt(0))
            assertEquals("Ireena", cursor.getString(1))
            assertEquals(3, cursor.getInt(2))
            assertEquals(24, cursor.getInt(3))
            assertEquals(0, cursor.getInt(4))

            assertTrue(cursor.moveToNext())
            assertEquals(CAMPAIGN_ID, cursor.getInt(0))
            assertEquals("Ismark", cursor.getString(1))
            assertEquals(4, cursor.getInt(2))
            assertEquals(31, cursor.getInt(3))
            assertEquals(1, cursor.getInt(4))
        }

        // The JSON column is gone, while the campaign itself and everything pointing at it stayed.
        assertTrue(
            "campaigns.participants must be dropped in version 8",
            !db.hasColumn("campaigns", "participants")
        )
        db.assertCampaignRowIntact(expectParticipantsColumn = false)
        db.assertSessionRowIntact()
        db.close()
    }

    @Test
    fun migrate7To8_survivesNullAndBrokenParticipantsJson() {
        helper.createDatabase(TEST_DB, 7).apply {
            // A readable roster next to the payloads that cannot be decoded: the good one must
            // still arrive, the bad ones must neither crash the migration nor produce rows.
            insertLegacyCampaign(participantsJson = CAMPAIGN_PARTICIPANTS_JSON)
            insertLegacyCampaign(id = 2, name = "Nullable roster", participantsJson = null)
            insertLegacyCampaign(id = 3, name = "Not even JSON", participantsJson = "}{ broken")
            insertLegacyCampaign(
                id = 4,
                name = "Wrong shape",
                participantsJson = """{"name":"Ireena"}"""
            )
            insertLegacyCampaign(
                id = 5,
                name = "Missing fields",
                participantsJson = """[{"name":"Ireena"}]"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, *ALL_MIGRATIONS)

        db.query("SELECT COUNT(*) FROM campaigns").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("every campaign must survive 7 -> 8", 5, cursor.getInt(0))
        }
        db.query("SELECT DISTINCT campaignId FROM campaign_participants").use { cursor ->
            assertEquals("only the decodable roster may produce rows", 1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(CAMPAIGN_ID, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To8_keepsDataAndEndsWithTheParticipantsTable() {
        helper.createDatabase(TEST_DB, 4).apply {
            insertCampaign()
            // session_events does not exist in v4, so no event can be seeded here.
            insertSession()
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, *ALL_MIGRATIONS)

        db.assertCampaignRowIntact(expectParticipantsColumn = false)
        db.assertSessionRowIntact()
        assertTrue("session_events table was not created", db.hasTable("session_events"))
        assertTrue(
            "campaign_participants table was not created",
            db.hasTable("campaign_participants")
        )
        assertTrue(
            "campaigns.participants must be dropped in version 8",
            !db.hasColumn("campaigns", "participants")
        )
        assertTrue(
            "session_events.participants is an event snapshot and must survive",
            db.hasColumn("session_events", "participants")
        )
        // A v4 campaign never had a roster, so there is nothing to carry over.
        db.query("SELECT COUNT(*) FROM campaign_participants").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migratedDatabase_opensWithRoom_andServesQueries() {
        helper.createDatabase(TEST_DB, 4).apply {
            insertCampaign()
            insertSession()
            close()
        }
        // Room must get the file closed, otherwise it reuses the helper's connection.
        helper.runMigrationsAndValidate(TEST_DB, 8, true, *ALL_MIGRATIONS).close()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            // Running real queries forces Room to open the file and compare the identity hash
            // of the migrated schema with the one generated from the entities.
            val campaign = runBlocking { room.campaignDao().getCampaignById(CAMPAIGN_ID) }
            assertNotNull("migrated campaign is not readable through Room", campaign)
            assertEquals(CAMPAIGN_NAME, campaign!!.campaign.name)
            assertEquals(CAMPAIGN_DESCRIPTION, campaign.campaign.description)
            assertEquals(CAMPAIGN_CREATED_AT, campaign.campaign.createdAt)
            assertTrue("a v4 campaign has no roster", campaign.participants.isEmpty())

            val sessions = runBlocking { room.sessionDao().getSessionsForCampaign(CAMPAIGN_ID).first() }
            assertEquals(1, sessions.size)
            assertEquals(SESSION_TITLE, sessions.single().title)

            val events = runBlocking { room.sessionEventDao().getEventsForSession(SESSION_ID).first() }
            assertTrue("session_events should still be empty", events.isEmpty())
        } finally {
            room.close()
        }
    }

    @Test
    fun migratedDatabase_servesTheRosterThroughRoom() {
        helper.createDatabase(TEST_DB, 7).apply {
            insertLegacyCampaign(participantsJson = CAMPAIGN_PARTICIPANTS_JSON)
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 8, true, *ALL_MIGRATIONS).close()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            // The migrated rows have to be reachable through the relation, in order.
            val campaign = runBlocking { room.campaignDao().getCampaignById(CAMPAIGN_ID) }
            assertNotNull("migrated campaign is not readable through Room", campaign)
            assertEquals(
                listOf("Ireena", "Ismark"),
                campaign!!.participants.sortedBy { it.orderIndex }.map { it.name }
            )

            val participants = runBlocking {
                room.campaignParticipantDao().getParticipantsForCampaign(CAMPAIGN_ID).first()
            }
            assertEquals(listOf(3, 4), participants.map { it.level })
            assertEquals(listOf(24, 31), participants.map { it.maxHp })
        } finally {
            room.close()
        }
    }

    // region helpers

    private fun SupportSQLiteDatabase.insertCampaign(
        id: Int = CAMPAIGN_ID,
        name: String = CAMPAIGN_NAME
    ) = execSQL(
        "INSERT INTO campaigns (id, name, description, createdAt) VALUES (?, ?, ?, ?)",
        arrayOf<Any>(id, name, CAMPAIGN_DESCRIPTION, CAMPAIGN_CREATED_AT)
    )

    /**
     * Seeds a campaign on a schema that still carries the JSON column, i.e. version 7 at the
     * latest. A `null` payload is written by leaving the nullable column out of the statement.
     */
    private fun SupportSQLiteDatabase.insertLegacyCampaign(
        id: Int = CAMPAIGN_ID,
        name: String = CAMPAIGN_NAME,
        participantsJson: String?
    ) {
        if (participantsJson == null) {
            insertCampaign(id, name)
            return
        }
        execSQL(
            """
            INSERT INTO campaigns (id, name, description, createdAt, participants)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any>(id, name, CAMPAIGN_DESCRIPTION, CAMPAIGN_CREATED_AT, participantsJson)
        )
    }

    private fun SupportSQLiteDatabase.insertSession() = execSQL(
        "INSERT INTO sessions (id, campaignId, title, notes, createdAt) VALUES (?, ?, ?, ?, ?)",
        arrayOf<Any>(SESSION_ID, CAMPAIGN_ID, SESSION_TITLE, SESSION_NOTES, SESSION_CREATED_AT)
    )

    private fun SupportSQLiteDatabase.insertSessionEvent() = execSQL(
        """
        INSERT INTO session_events (id, sessionId, type, title, content, orderIndex, timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any>(
            EVENT_ID,
            SESSION_ID,
            EVENT_TYPE,
            EVENT_TITLE,
            EVENT_CONTENT,
            EVENT_ORDER_INDEX,
            EVENT_TIMESTAMP
        )
    )

    private fun SupportSQLiteDatabase.assertCampaignRowIntact(expectParticipantsColumn: Boolean) {
        query("SELECT * FROM campaigns").use { cursor ->
            assertEquals("campaign row did not survive the migration", 1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(CAMPAIGN_ID, cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            assertEquals(CAMPAIGN_NAME, cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(
                CAMPAIGN_DESCRIPTION,
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            )
            assertEquals(
                CAMPAIGN_CREATED_AT,
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            )
            if (expectParticipantsColumn) {
                assertTrue(
                    "participants must be NULL for rows that existed before the migration",
                    cursor.isNull(cursor.getColumnIndexOrThrow("participants"))
                )
            }
        }
    }

    private fun SupportSQLiteDatabase.assertSessionRowIntact() {
        query("SELECT * FROM sessions").use { cursor ->
            assertEquals("session row did not survive the migration", 1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(SESSION_ID, cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            assertEquals(CAMPAIGN_ID, cursor.getInt(cursor.getColumnIndexOrThrow("campaignId")))
            assertEquals(SESSION_TITLE, cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(SESSION_NOTES, cursor.getString(cursor.getColumnIndexOrThrow("notes")))
            assertEquals(
                SESSION_CREATED_AT,
                cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
            )
        }
    }

    private fun SupportSQLiteDatabase.hasTable(table: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'")
            .use { cursor -> cursor.count == 1 }

    private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }

    // endregion

    private companion object {
        const val TEST_DB = "migration-test.db"

        const val CAMPAIGN_ID = 1
        const val CAMPAIGN_NAME = "Curse of Strahd"
        const val CAMPAIGN_DESCRIPTION = "Gothic horror in Barovia"
        const val CAMPAIGN_CREATED_AT = 1_700_000_000_000L

        /** The version 7 payload of `campaigns.participants`: an EventParticipant list. */
        const val CAMPAIGN_PARTICIPANTS_JSON =
            """[{"name":"Ireena","level":3,"maxHp":24},{"name":"Ismark","level":4,"maxHp":31}]"""

        const val SESSION_ID = 1
        const val SESSION_TITLE = "Session 1: Into the mists"
        const val SESSION_NOTES = "The party crossed the border"
        const val SESSION_CREATED_AT = 1_700_000_100_000L

        const val EVENT_ID = 1
        const val EVENT_TYPE = "COMBAT" // SessionEventType is stored by enum name
        const val EVENT_TITLE = "Wolves at the gate"
        const val EVENT_CONTENT = "Four dire wolves ambushed the party"
        const val EVENT_ORDER_INDEX = 0
        const val EVENT_TIMESTAMP = 1_700_000_200_000L
        const val EVENT_PARTICIPANTS = """[{"name":"Ireena"}]"""
    }
}
