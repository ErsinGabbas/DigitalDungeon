package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.CampaignEntity
import com.example.digitaldungeon.domain.model.Campaign

fun CampaignEntity.toDomain(): Campaign =
    Campaign(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )

fun Campaign.toEntity() =
    CampaignEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )