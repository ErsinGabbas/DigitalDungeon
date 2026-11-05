package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.mapper.toDomain
import com.example.digitaldungeon.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao
) : SessionRepository {

    override fun getSessionsForCampaign(
        campaignId: Int
    ): Flow<List<Session>> =
        dao.getSessionsForCampaign(campaignId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    override fun getSessionById(
        id: Int
    ): Flow<Session?> =
        dao.getSessionById(id)
            .map { entity ->

                entity?.toDomain()

            }

    override suspend fun addSession(
        campaignId: Int,
        title: String,
        notes: String
    ) {

        dao.insertSession(
            SessionEntity(
                campaignId = campaignId,
                title = title,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateSession(
        id: Int,
        title: String,
        notes: String
    ) {

        dao.updateSession(id, title, notes)

    }

    override suspend fun deleteSession(
        id: Int
    ) {

        dao.deleteSession(id)

    }
}
