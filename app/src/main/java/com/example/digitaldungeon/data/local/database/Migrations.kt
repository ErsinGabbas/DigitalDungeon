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
        // TODO: Implement actual migration logic when schema changes are known
        // For now, this is a placeholder to prevent crashes
    }
}

/**
 * Migration from version 4 to 5
 * Use this for future schema changes
 * 
 * When adding new columns or tables:
 * 1. Update the entity classes
 * 2. Increment database version
 * 3. Add migration logic here
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add future migration logic here
        // Example:
        // database.execSQL("ALTER TABLE sessions ADD COLUMN new_field TEXT")
    }
}
