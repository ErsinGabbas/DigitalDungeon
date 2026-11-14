package com.example.digitaldungeon.di

import android.content.Context
import androidx.room.Room
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.CampaignParticipantDao
import com.example.digitaldungeon.data.local.dao.QuestConsequenceDao
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.local.dao.QuestNoteDao
import com.example.digitaldungeon.data.local.dao.QuestObjectiveDao
import com.example.digitaldungeon.data.local.dao.QuestRelationDao
import com.example.digitaldungeon.data.local.dao.QuestRewardDao
import com.example.digitaldungeon.data.local.dao.QuestSessionLinkDao
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.database.ALL_MIGRATIONS
import com.example.digitaldungeon.data.local.database.DESTRUCTIVELY_MIGRATED_VERSIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "digital_dungeon_db"
        )
            .addMigrations(*ALL_MIGRATIONS)
            // Schemas for versions 1-2 were never exported, so no migration path exists.
            // Such installations are recreated from scratch; the 3 -> 13 path stays migrational.
            .fallbackToDestructiveMigrationFrom(*DESTRUCTIVELY_MIGRATED_VERSIONS)
            .build()
    }

    @Provides
    fun provideCampaignDao(database: AppDatabase): CampaignDao {
        return database.campaignDao()
    }

    @Provides
    fun provideCampaignParticipantDao(database: AppDatabase): CampaignParticipantDao {
        return database.campaignParticipantDao()
    }

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    fun provideSessionEventDao(database: AppDatabase): SessionEventDao {
        return database.sessionEventDao()
    }

    @Provides
    fun provideQuestDao(database: AppDatabase): QuestDao {
        return database.questDao()
    }

    @Provides
    fun provideQuestObjectiveDao(database: AppDatabase): QuestObjectiveDao {
        return database.questObjectiveDao()
    }

    @Provides
    fun provideQuestRewardDao(database: AppDatabase): QuestRewardDao {
        return database.questRewardDao()
    }

    @Provides
    fun provideQuestConsequenceDao(database: AppDatabase): QuestConsequenceDao {
        return database.questConsequenceDao()
    }

    @Provides
    fun provideQuestNoteDao(database: AppDatabase): QuestNoteDao {
        return database.questNoteDao()
    }

    @Provides
    fun provideQuestRelationDao(database: AppDatabase): QuestRelationDao {
        return database.questRelationDao()
    }

    @Provides
    fun provideQuestHistoryDao(database: AppDatabase): QuestHistoryDao {
        return database.questHistoryDao()
    }

    @Provides
    fun provideQuestSessionLinkDao(database: AppDatabase): QuestSessionLinkDao {
        return database.questSessionLinkDao()
    }
}
