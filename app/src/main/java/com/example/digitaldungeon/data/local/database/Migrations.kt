package com.example.digitaldungeon.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 3 to 4
 * 
 * IMPORTANT: This migration is currently empty because we don't know the exact
 * schema changes that occurred between versions 3 and 4. When implementing
 * actual migrations, you need to:
 * 
 * 1. Know the previous schema (version 3)
 * 2. Know the new schema (version 4)
 * 3. Write SQL statements to transform the old schema to the new schema
 * 
 * Example migration:
 * database.execSQL("ALTER TABLE campaigns ADD COLUMN new_column TEXT")
 * 
 * For production apps, consider using Room's AutoMigration or export schemas
 * to track changes properly.
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
