package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestObjectiveEntity
import com.example.digitaldungeon.domain.model.QuestObjective

fun QuestObjectiveEntity.toDomain(): QuestObjective =
    QuestObjective(
        id = id,
        questId = questId,
        title = title,
        description = description,
        status = status,
        orderIndex = orderIndex,
        isOptional = isOptional
    )

fun QuestObjective.toEntity() =
    QuestObjectiveEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        status = status,
        orderIndex = orderIndex,
        isOptional = isOptional
    )
