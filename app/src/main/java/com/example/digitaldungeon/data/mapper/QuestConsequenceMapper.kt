package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestConsequenceEntity
import com.example.digitaldungeon.domain.model.QuestConsequence

fun QuestConsequenceEntity.toDomain(): QuestConsequence =
    QuestConsequence(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type
    )

fun QuestConsequence.toEntity() =
    QuestConsequenceEntity(
        id = id,
        questId = questId,
        title = title,
        description = description,
        type = type
    )
