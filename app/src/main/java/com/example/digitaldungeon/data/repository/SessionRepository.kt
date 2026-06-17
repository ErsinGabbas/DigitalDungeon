package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.data.local.dao.SessionDao
import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.data.mapper.toDomain
import kotlinx.coroutines.flow.map

class SessionRepository(
    private val dao: SessionDao
) {

    fun getSessionsForCampaign(
        campaignId: Int
    ) =
        dao.getSessionsForCampaign(campaignId)
            .map { entities ->

                entities.map {
                    it.toDomain()
                }

            }

    suspend fun addSession(
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

    suspend fun deleteSession(
        id: Int
    ) {

        dao.deleteSession(id)

    }
}