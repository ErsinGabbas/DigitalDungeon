package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestRewardEntity
import com.example.digitaldungeon.domain.model.QuestReward

fun QuestRewardEntity.toDomain(): QuestReward =
    QuestReward(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type
    )

fun QuestReward.toEntity() =
    QuestRewardEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type
    )
