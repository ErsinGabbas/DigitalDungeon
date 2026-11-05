package com.example.digitaldungeon.data.repository

import com.example.digitaldungeon.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun getSessionsForCampaign(
        campaignId: Int
    ): Flow<List<Session>>

    fun getSessionById(
        id: Int
    ): Flow<Session?>

    suspend fun addSession(
        campaignId: Int,
        title: String,
        notes: String
    )

    suspend fun deleteSession(
        id: Int
    )
}
