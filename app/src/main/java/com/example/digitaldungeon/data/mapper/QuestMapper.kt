package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestEntity
import com.example.digitaldungeon.domain.model.Quest

fun QuestEntity.toDomain(): Quest =
    Quest(
        id = id,
        campaignId = campaignId,
        title = title,
        description = description,
        category = category,
        status = status,
        priority = priority,
        isHidden = isHidden,
        createdAt = createdAt,
        inGameDate = inGameDate
    )

fun Quest.toEntity() =
    QuestEntity(
        id = id,
        campaignId = campaignId,
        title = title,
        description = description,
        category = category,
        status = status,
        priority = priority,
        isHidden = isHidden,
        createdAt = createdAt,
        inGameDate = inGameDate
    )
