package com.example.digitaldungeon.data.repository

import androidx.room.withTransaction
import com.example.digitaldungeon.data.local.dao.QuestDao
import com.example.digitaldungeon.data.local.dao.QuestSessionLinkDao
import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.database.AppDatabase
import com.example.digitaldungeon.data.local.entity.QuestSessionLinkEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.QuestSessionLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuestSessionLinkRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: QuestSessionLinkDao,
    private val questDao: QuestDao,
    private val sessionDao: SessionDao
) : QuestSessionLinkRepository {

    override fun getLinksForQuest(
        questId: Int
    ): Flow<List<QuestSessionLink>> =
        dao.getLinksForQuest(questId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override fun getLinksForSession(
        sessionId: Int
    ): Flow<List<QuestSessionLink>> =
        dao.getLinksForSession(sessionId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override suspend fun addLink(
        questId: Int,
        sessionId: Int
    ): AddSessionLinkResult =
        database.withTransaction {

            val questCampaign = questDao.getCampaignIdForQuest(questId)
                ?: return@withTransaction AddSessionLinkResult.QuestMissing

            val sessionCampaign = sessionDao.getCampaignIdForSession(sessionId)
                ?: return@withTransaction AddSessionLinkResult.SessionMissing

            if (questCampaign != sessionCampaign) {
                return@withTransaction AddSessionLinkResult.DifferentCampaigns
            }

            // Checked rather than left to the unique index, so that a second attempt comes back as
            // an answer the screen can show instead of an exception it would have to interpret.
            if (dao.countLink(questId, sessionId) > 0) {
                return@withTransaction AddSessionLinkResult.AlreadyExists
            }

            dao.insertLink(
                QuestSessionLinkEntity(
                    questId = questId,
                    sessionId = sessionId
                )
            )

            AddSessionLinkResult.Added
        }

    override suspend fun deleteLink(
        id: Int
    ) {

        dao.deleteLink(id)

    }
}
