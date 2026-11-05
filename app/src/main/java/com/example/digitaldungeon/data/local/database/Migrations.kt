package com.example.digitaldungeon.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 3 to 4
 * No-op: schema for version 3 was never exported, so no changes can be replayed.
 * Kept so that the 3 -> 7 migration path stays connected.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // No schema changes
    }
}

/**
 * Migration from version 4 to 5
 * Adds session_events table for timeline-based session events
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create session_events table
        database.execSQL("""
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
        database.execSQL("CREATE INDEX IF NOT EXISTS index_session_events_sessionId ON session_events(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_session_events_sessionId_orderIndex ON session_events(sessionId, orderIndex)")
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
 * Every migration the app ships with, in one place so that the Hilt provider and
 * the migration tests cannot drift apart.
 */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7
)

/**
 * Versions with no migration path: their schemas were never exported, so such
 * databases are recreated from scratch instead of crashing on open.
 */
val DESTRUCTIVELY_MIGRATED_VERSIONS = intArrayOf(1, 2)
