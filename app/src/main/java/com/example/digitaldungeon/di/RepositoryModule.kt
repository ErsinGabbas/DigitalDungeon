package com.example.digitaldungeon.di

import com.example.digitaldungeon.data.backup.BackupService
import com.example.digitaldungeon.data.backup.BackupServiceImpl
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.CampaignRepositoryImpl
import com.example.digitaldungeon.data.repository.SessionEventRepository
import com.example.digitaldungeon.data.repository.SessionEventRepositoryImpl
import com.example.digitaldungeon.data.repository.SessionRepository
import com.example.digitaldungeon.data.repository.SessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(impl: CampaignRepositoryImpl): CampaignRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSessionEventRepository(impl: SessionEventRepositoryImpl): SessionEventRepository

    @Binds
    @Singleton
    abstract fun bindBackupService(impl: BackupServiceImpl): BackupService
}
