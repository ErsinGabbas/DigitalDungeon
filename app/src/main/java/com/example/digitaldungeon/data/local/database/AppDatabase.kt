package com.example.digitaldungeon.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.CampaignParticipantDao
import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.data.local.entity.CampaignParticipantEntity
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.entity.SessionEventEntity
import com.example.digitaldungeon.data.local.dao.SessionEventDao


@Database(
    entities = [
        CampaignEntity::class,
        CampaignParticipantEntity::class,
        SessionEntity::class,
        SessionEventEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun campaignDao(): CampaignDao

    abstract fun campaignParticipantDao(): CampaignParticipantDao

    abstract fun sessionDao(): SessionDao

    abstract fun sessionEventDao(): SessionEventDao
}
