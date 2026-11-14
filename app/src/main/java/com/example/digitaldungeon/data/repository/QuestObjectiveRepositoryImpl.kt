package com.example.digitaldungeon.data.repository

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.QuestHistoryDao
import com.example.digitaldungeon.data.local.dao.QuestObjectiveDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestHistoryEventType
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestObjectiveRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: QuestObjectiveDao,
    private val historyDao: QuestHistoryDao
) : QuestObjectiveRepository {

    override fun getObjectivesForQuest(
        questId: Int
    ): Flow<List<QuestObjective>> =
        dao.getObjectivesForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addObjective(
        questId: Int,
        title: String,
        description: String,
        isOptional: Boolean
    ) {

        // -1 for a quest with no objectives yet, so the first one lands at zero. Read from the
        // table and not from any list in memory: that is what makes two quick additions land on
        // two different positions.
        val maxOrderIndex = dao.getMaxOrderIndex(questId) ?: -1

        dao.insertObjective(
            QuestObjectiveEntity(
                questId = questId,
                title = title,
                description = description,
                status = QuestObjectiveStatus.NOT_STARTED,
                orderIndex = maxOrderIndex + 1,
                isOptional = isOptional
            )
        )
    }

    override suspend fun updateObjective(
        id: Int,
        title: String,
        description: String,
        isOptional: Boolean
    ) {

        dao.updateObjective(
            id = id,
            title = title,
            description = description,
            isOptional = isOptional
        )
    }

    /**
     * Moves one step and records it in the quest's history, in one transaction.
     *
     * The entry names the step by its title, which is read from the row being changed rather than
     * taken from the caller: the UI has the title on screen, but a log that trusted it would record
     * whatever the caller said happened instead of what did.
     *
     * A step that is gone is simply not recorded. Its row is already absent, so there is nothing to
     * move and nothing true to write down.
     */
    override suspend fun updateObjectiveStatus(
        id: Int,
        status: QuestObjectiveStatus
    ) {

        database.withTransaction {

            val objective = dao.getObjectiveById(id) ?: return@withTransaction

            dao.updateObjectiveStatus(id, status)

            historyDao.insertEntry(
                QuestHistoryEntity(
                    questId = objective.questId,
                    eventType = status.toHistoryEventType(),
                    description = objective.title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun moveObjective(
        id: Int,
        direction: QuestObjectiveRepository.MoveDirection
    ) {

        dao.moveObjective(
            objectiveId = id,
            moveUp = direction == QuestObjectiveRepository.MoveDirection.UP
        )
    }

    override suspend fun deleteObjective(
        id: Int
    ) {

        dao.deleteObjective(id)

    }

    /**
     * Which kind of history entry a move to this status leaves behind.
     *
     * Moving a step back to [QuestObjectiveStatus.NOT_STARTED] is recorded as *reopened* rather
     * than as "not started": the step did start once, and the log is about what the DM did, not
     * about where the step now stands.
     */
    private fun QuestObjectiveStatus.toHistoryEventType(): QuestHistoryEventType =
        when (this) {
            QuestObjectiveStatus.COMPLETED -> QuestHistoryEventType.OBJECTIVE_COMPLETED
            QuestObjectiveStatus.FAILED -> QuestHistoryEventType.OBJECTIVE_FAILED
            QuestObjectiveStatus.NOT_STARTED -> QuestHistoryEventType.OBJECTIVE_REOPENED
        }
}
