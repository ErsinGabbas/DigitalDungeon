package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.SessionEntity
import com.example.digitaldungeon.domain.model.Session

fun SessionEntity.toDomain(): Session =
    Session(
        id = id,
        campaignId = campaignId,
        title = title,
        notes = notes,
        createdAt = createdAt
    )

fun Session.toEntity() =
    SessionEntity(
        id = id,
        campaignId = campaignId,
        title = title,
        notes = notes,
        createdAt = createdAt
    )