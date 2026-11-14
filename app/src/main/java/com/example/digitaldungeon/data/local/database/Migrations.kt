package com.example.digitaldungeon.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.digitaldungeon.domain.model.EventParticipant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Migration from version 3 to 4
 * No-op: schema for version 3 was never exported, so no changes can be replayed.
 * Kept so that the 3 -> 13 migration path stays connected.
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
 * Migration from version 8 to 9
 * Adds the `quests` table: the first table of the quest tracker.
 *
 * Purely additive, which is what makes it safe - no existing table is read, rewritten or dropped,
 * so every campaign, session and event is untouched by construction. The column list has to match
 * what Room generates from `QuestEntity` exactly, including the `ON UPDATE NO ACTION` that Room
 * writes into every foreign key clause, otherwise `runMigrationsAndValidate` refuses the result.
 *
 * `category`, `status` and `priority` are TEXT because that is how Room persists an enum: the
 * constant name goes into the column. `isHidden` is INTEGER, which is how SQLite stores a boolean.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                campaignId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                status TEXT NOT NULL,
                priority TEXT NOT NULL,
                isHidden INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                inGameDate TEXT,
                FOREIGN KEY(campaignId) REFERENCES campaigns(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quests_campaignId ON quests (campaignId)"
        )
    }
}

/**
 * Migration from version 9 to 10
 * Adds the `quest_objectives` table: the steps of a quest.
 *
 * Additive like 8 -> 9, so nothing that already exists is read or rewritten. The foreign key
 * points at `quests`, which itself cascades from `campaigns`, so deleting a campaign now reaches
 * three tables deep without leaving anything behind.
 *
 * The column list, the `ON UPDATE NO ACTION` and both index names have to match what Room
 * generates from `QuestObjectiveEntity` exactly, or `runMigrationsAndValidate` refuses the result.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_objectives (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                status TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                isOptional INTEGER NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_objectives_questId " +
                "ON quest_objectives (questId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_objectives_questId_orderIndex " +
                "ON quest_objectives (questId, orderIndex)"
        )
    }
}

/**
 * Migration from version 10 to 11
 * Adds the three tables that record what a quest is worth and what it left behind: `quest_rewards`,
 * `quest_consequences` and `quest_notes`.
 *
 * One migration for three tables because they arrive together and none of them depends on the
 * others; three separate versions would buy nothing and cost two more steps on the upgrade path.
 * Additive like 8 -> 9 and 9 -> 10, so nothing that already exists is read or rewritten.
 *
 * All three hang off `quests`, which cascades from `campaigns`, so deleting a campaign now reaches
 * five tables and leaves none of them behind. The column lists, the `ON UPDATE NO ACTION` and the
 * index names have to match what Room generates from the entities exactly, or
 * `runMigrationsAndValidate` refuses the result.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_rewards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                type TEXT NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_rewards_questId ON quest_rewards (questId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_consequences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                type TEXT NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_consequences_questId " +
                "ON quest_consequences (questId)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_notes_questId ON quest_notes (questId)"
        )
    }
}

/**
 * Migration from version 11 to 12
 * Adds `quest_relations`: the links between quests.
 *
 * Additive like the three before it. The table carries two foreign keys into the same parent -
 * both ends are quests - and both cascade, so deleting a quest takes every link that mentions it,
 * from either side. That is what keeps a link from ever pointing at a quest that is gone.
 *
 * The third index is UNIQUE. It is the last word on duplicates: the repository checks first and
 * reports a duplicate as an ordinary answer, but the constraint is what makes two rows stating the
 * same fact impossible rather than merely unlikely. Its name has to match what Room generates
 * (`index_quest_relations_sourceQuestId_targetQuestId_relationType`), index names included, or
 * `runMigrationsAndValidate` refuses the result.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_relations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sourceQuestId INTEGER NOT NULL,
                targetQuestId INTEGER NOT NULL,
                relationType TEXT NOT NULL,
                FOREIGN KEY(sourceQuestId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(targetQuestId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_relations_sourceQuestId " +
                "ON quest_relations (sourceQuestId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_relations_targetQuestId " +
                "ON quest_relations (targetQuestId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "index_quest_relations_sourceQuestId_targetQuestId_relationType " +
                "ON quest_relations (sourceQuestId, targetQuestId, relationType)"
        )
    }
}

/**
 * Migration from version 12 to 13
 * Adds `quest_history`, the append-only log of what was done to a quest, and
 * `quest_session_links`, which records that a quest came up in a session.
 *
 * The last additive migration of the quest tracker, and the first one to reach outside it:
 * `quest_session_links` has a foreign key into `sessions` as well as into `quests`, so a deleted
 * session now takes its links with it just as a deleted quest does. Nothing that already exists is
 * read or rewritten, and in particular `session_events` - the busiest table in the app - is not
 * touched at all, which is the whole reason the link is a table rather than a column on it.
 *
 * The column lists, the `ON UPDATE NO ACTION` and every index name have to match what Room
 * generates from the two entities exactly, or `runMigrationsAndValidate` refuses the result.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                eventType TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_history_questId ON quest_history (questId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_history_questId_timestamp " +
                "ON quest_history (questId, timestamp)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quest_session_links (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                questId INTEGER NOT NULL,
                sessionId INTEGER NOT NULL,
                FOREIGN KEY(questId) REFERENCES quests(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(sessionId) REFERENCES sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_session_links_questId " +
                "ON quest_session_links (questId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quest_session_links_sessionId " +
                "ON quest_session_links (sessionId)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_quest_session_links_questId_sessionId " +
                "ON quest_session_links (questId, sessionId)"
        )
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
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13
)

/**
 * Versions with no migration path: their schemas were never exported, so such
 * databases are recreated from scratch instead of crashing on open.
 */
val DESTRUCTIVELY_MIGRATED_VERSIONS = intArrayOf(1, 2)
