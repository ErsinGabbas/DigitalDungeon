package com.example.digitaldungeon.data.repository

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Most writes here touch a single table and need no transaction of their own. The exception is
 * [updateQuestStatus], which also appends to the quest's history: a status that moved without
 * leaving an entry, or an entry with no status behind it, would each be worse than no log at all,
 * so the two land together or not at all.
 */
class QuestRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: QuestDao,
    private val historyDao: QuestHistoryDao
) : QuestRepository {

    override fun getQuestsForCampaign(
        campaignId: Int
    ): Flow<List<Quest>> =
        dao.getQuestsForCampaign(campaignId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override fun getQuestById(
        id: Int
    ): Flow<Quest?> =
        dao.getQuestById(id)
            .map { entity ->

                entity?.toDomain()

            }

    override suspend fun addQuest(
        campaignId: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    ) {

        dao.insertQuest(
            QuestEntity(
                campaignId = campaignId,
                title = title,
                description = description,
                category = category,
                status = status,
                priority = priority,
                isHidden = isHidden,
                createdAt = System.currentTimeMillis(),
                inGameDate = inGameDate
            )
        )
    }

    override suspend fun updateQuest(
        id: Int,
        title: String,
        description: String,
        category: QuestCategory,
        status: QuestStatus,
        priority: QuestPriority,
        isHidden: Boolean,
        inGameDate: String?
    ) {

        dao.updateQuest(
            id = id,
            title = title,
            description = description,
            category = category,
            status = status,
            priority = priority,
            isHidden = isHidden,
            inGameDate = inGameDate
        )
    }

    override suspend fun updateQuestStatus(
        id: Int,
        status: QuestStatus
    ) {

        database.withTransaction {

            dao.updateQuestStatus(id, status)

            historyDao.insertEntry(
                QuestHistoryEntity(
                    questId = id,
                    eventType = QuestHistoryEventType.QUEST_STATUS_CHANGED,
                    // The constant name and not a sentence: the log is read in whatever language
                    // the app is in when it is read, not the one it was written in.
                    description = status.name,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteQuest(
        id: Int
    ) {

        dao.deleteQuest(id)

    }
}
