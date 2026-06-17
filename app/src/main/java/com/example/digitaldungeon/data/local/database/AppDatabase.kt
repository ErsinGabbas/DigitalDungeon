package com.example.digitaldungeon.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import android.content.Context
import androidx.room.Room
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.dao.SessionDao


@Database(
    entities = [
        CampaignEntity::class,
        SessionEntity::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun campaignDao(): CampaignDao

    abstract fun sessionDao(): SessionDao

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
                    .addMigrations(MIGRATION_3_4)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}

