package com.example.digitaldungeon.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Migration from version 3 to 4
 * No-op: schema for version 3 was never exported, so no changes can be replayed.
 * Kept so that the 3 -> 8 migration path stays connected.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No schema changes
    }
}

/**
 * Migration from version 4 to 5
 * Adds session_events table for timeline-based session events
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create session_events table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS session_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                type TEXT NOT NULL,
                title TEXT,
                content TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES sessions(id) ON DELETE CASCADE
            )
        """)
        
        // Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_events_sessionId ON session_events(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_session_events_sessionId_orderIndex ON session_events(sessionId, orderIndex)")
    }
}

/**
 * Migration from version 5 to 6
 * Adds participants column to session_events table for storing character snapshots
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE session_events 
            ADD COLUMN participants TEXT
        """)
    }
}

/**
 * Migration from version 6 to 7
 * Adds participants column to campaigns table for storing campaign participants
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            ALTER TABLE campaigns
            ADD COLUMN participants TEXT
        """)
    }
}

/**
 * Json used only to replay the version 7 payload of `campaigns.participants`.
 * Deliberately local to this file: a migration has to keep decoding the format that was on disk
 * back then, independently of how the mappers evolve.
 */
private val migrationJson = Json { ignoreUnknownKeys = true }

/**
 * Migration from version 7 to 8
 * Moves campaign participants out of the `campaigns.participants` JSON blob into the queryable
 * `campaign_participants` table and drops the column.
 *
 * `ALTER TABLE ... DROP COLUMN` needs SQLite 3.35 while minSdk is 24, so `campaigns` is rewritten
 * with Room's canonical recipe (create -> copy -> drop -> rename). Room runs migrations with
 * foreign keys disabled and only then issues `PRAGMA foreign_key_check`, so dropping a table that
 * `sessions` and `campaign_participants` reference is safe as long as the name is restored before
 * the migration ends. `campaigns` is dropped rather than renamed away, because since SQLite 3.25
 * `ALTER TABLE ... RENAME TO` also rewrites the foreign key clauses pointing at the old name.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. The new, queryable home of campaign rosters.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS campaign_participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                campaignId INTEGER NOT NULL,
                name TEXT NOT NULL,
                level INTEGER NOT NULL,
                maxHp INTEGER NOT NULL,
                orderIndex INTEGER NOT NULL,
                FOREIGN KEY(campaignId) REFERENCES campaigns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_campaign_participants_campaignId " +
                "ON campaign_participants (campaignId)"
        )

        // 2. Replay the JSON rosters into rows, keeping the list order as orderIndex.
        //    A NULL, empty or broken payload is simply skipped: losing an unreadable roster is
        //    acceptable, failing the migration of the whole database is not.
        db.query("SELECT id, participants FROM campaigns").use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val participantsIndex = cursor.getColumnIndexOrThrow("participants")

            while (cursor.moveToNext()) {
                val campaignId = cursor.getInt(idIndex)
                val raw = if (cursor.isNull(participantsIndex)) {
                    null
                } else {
                    cursor.getString(participantsIndex)
                }

                decodeLegacyParticipants(raw).forEachIndexed { orderIndex, participant ->
                    db.execSQL(
                        """
                        INSERT INTO campaign_participants
                            (campaignId, name, level, maxHp, orderIndex)
                        VALUES (?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            campaignId,
                            participant.name,
                            participant.level,
                            participant.maxHp,
                            orderIndex
                        )
                    )
                }
            }
        }

        // 3. Rewrite `campaigns` without the participants column.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS _new_campaigns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO _new_campaigns (id, name, description, createdAt)
            SELECT id, name, description, createdAt FROM campaigns
            """.trimIndent()
        )
        db.execSQL("DROP TABLE campaigns")
        db.execSQL("ALTER TABLE _new_campaigns RENAME TO campaigns")
        // `campaigns` carries no indices, so there is nothing to recreate here.
    }

    private fun decodeLegacyParticipants(raw: String?): List<EventParticipant> {
        if (raw.isNullOrBlank()) return emptyList()

        return try {
            migrationJson.decodeFromString<List<EventParticipant>>(raw)
        } catch (e: SerializationException) {
            // Malformed JSON, the wrong shape, or an EventParticipant field that is missing.
            emptyList()
        }
    }
}

/**
 * Every migration the app ships with, in one place so that the Hilt provider and
 * the migration tests cannot drift apart.
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8
)

/**
 * Versions with no migration path: their schemas were never exported, so such
 * databases are recreated from scratch instead of crashing on open.
 */
val DESTRUCTIVELY_MIGRATED_VERSIONS = intArrayOf(1, 2)
