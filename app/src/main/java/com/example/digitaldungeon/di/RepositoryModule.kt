package com.example.digitaldungeon.di

import com.example.digitaldungeon.data.backup.BackupService
import com.example.digitaldungeon.data.backup.BackupServiceImpl
import com.example.digitaldungeon.data.repository.CampaignRepository
import com.example.digitaldungeon.data.repository.CampaignRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestConsequenceRepository
import com.example.digitaldungeon.data.repository.QuestConsequenceRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestNoteRepository
import com.example.digitaldungeon.data.repository.QuestNoteRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestObjectiveRepository
import com.example.digitaldungeon.data.repository.QuestHistoryRepository
import com.example.digitaldungeon.data.repository.QuestHistoryRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRelationRepository
import com.example.digitaldungeon.data.repository.QuestRelationRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestObjectiveRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRewardRepository
import com.example.digitaldungeon.data.repository.QuestRewardRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepository
import com.example.digitaldungeon.data.repository.QuestSessionLinkRepositoryImpl
import com.example.digitaldungeon.data.repository.QuestRepository
import com.example.digitaldungeon.data.repository.QuestRepositoryImpl
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
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    @Singleton
    abstract fun bindQuestObjectiveRepository(
        impl: QuestObjectiveRepositoryImpl
    ): QuestObjectiveRepository

    @Binds
    @Singleton
    abstract fun bindQuestRewardRepository(
        impl: QuestRewardRepositoryImpl
    ): QuestRewardRepository

    @Binds
    @Singleton
    abstract fun bindQuestConsequenceRepository(
        impl: QuestConsequenceRepositoryImpl
    ): QuestConsequenceRepository

    @Binds
    @Singleton
    abstract fun bindQuestNoteRepository(
        impl: QuestNoteRepositoryImpl
    ): QuestNoteRepository

    @Binds
    @Singleton
    abstract fun bindQuestRelationRepository(
        impl: QuestRelationRepositoryImpl
    ): QuestRelationRepository

    @Binds
    @Singleton
    abstract fun bindQuestHistoryRepository(
        impl: QuestHistoryRepositoryImpl
    ): QuestHistoryRepository

    @Binds
    @Singleton
    abstract fun bindQuestSessionLinkRepository(
        impl: QuestSessionLinkRepositoryImpl
    ): QuestSessionLinkRepository

    @Binds
    @Singleton
    abstract fun bindBackupService(impl: BackupServiceImpl): BackupService
}
