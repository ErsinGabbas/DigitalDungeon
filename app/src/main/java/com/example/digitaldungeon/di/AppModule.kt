package com.example.digitaldungeon.di

import android.content.Context
import androidx.room.Room
import com.example.digitaldungeon.data.local.dao.CampaignDao
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.dao.SessionEventDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.database.ALL_MIGRATIONS
import com.example.digitaldungeon.data.local.database.DESTRUCTIVELY_MIGRATED_VERSIONS
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.data.repository.SessionEventRepository
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
            // Such installations are recreated from scratch; the 3 -> 7 path stays migrational.
            .fallbackToDestructiveMigrationFrom(*DESTRUCTIVELY_MIGRATED_VERSIONS)
            .build()
    }

    @Provides
    fun provideCampaignDao(database: AppDatabase): CampaignDao {
        return database.campaignDao()
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
    @Singleton
    fun provideCampaignRepository(campaignDao: CampaignDao): CampaignRepository {
        return CampaignRepository(campaignDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(sessionDao: SessionDao): SessionRepository {
        return SessionRepository(sessionDao)
    }

    @Provides
    @Singleton
    fun provideSessionEventRepository(sessionEventDao: SessionEventDao): SessionEventRepository {
        return SessionEventRepository(sessionEventDao)
    }
}
