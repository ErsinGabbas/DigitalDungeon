package com.example.digitaldungeon.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import android.content.Context
import androidx.room.Room
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.local.dao.SessionEventDao


@Database(
    entities = [
        CampaignEntity::class,
        SessionEntity::class,
        SessionEventEntity::class
    ],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun campaignDao(): CampaignDao

    abstract fun sessionDao(): SessionDao

    abstract fun sessionEventDao(): SessionEventDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_dungeon_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}

