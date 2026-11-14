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
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.data.local.dao.QuestObjectiveDao
import com.example.digitaldungeon.data.local.entity.QuestRewardEntity
import com.example.digitaldungeon.data.local.dao.QuestRewardDao
import com.example.digitaldungeon.data.local.entity.QuestConsequenceEntity
import com.example.digitaldungeon.data.local.dao.QuestConsequenceDao
import com.example.digitaldungeon.data.local.entity.QuestNoteEntity
import com.example.digitaldungeon.data.local.dao.QuestNoteDao
import com.example.digitaldungeon.data.local.entity.QuestRelationEntity
import com.example.digitaldungeon.data.local.dao.QuestRelationDao
import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.local.entity.QuestSessionLinkEntity
import com.example.digitaldungeon.data.local.dao.QuestSessionLinkDao


@Database(
    entities = [
        CampaignEntity::class,
        CampaignParticipantEntity::class,
        SessionEntity::class,
        SessionEventEntity::class,
        QuestEntity::class,
        QuestObjectiveEntity::class,
        QuestRewardEntity::class,
        QuestConsequenceEntity::class,
        QuestNoteEntity::class,
        QuestRelationEntity::class,
        QuestHistoryEntity::class,
        QuestSessionLinkEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun campaignDao(): CampaignDao

    abstract fun campaignParticipantDao(): CampaignParticipantDao

    abstract fun sessionDao(): SessionDao

    abstract fun sessionEventDao(): SessionEventDao

    abstract fun questDao(): QuestDao

    abstract fun questObjectiveDao(): QuestObjectiveDao

    abstract fun questRewardDao(): QuestRewardDao

    abstract fun questConsequenceDao(): QuestConsequenceDao

    abstract fun questNoteDao(): QuestNoteDao

    abstract fun questRelationDao(): QuestRelationDao

    abstract fun questHistoryDao(): QuestHistoryDao

    abstract fun questSessionLinkDao(): QuestSessionLinkDao
}
