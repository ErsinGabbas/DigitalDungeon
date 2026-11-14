package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestRelationEntity
import com.example.digitaldungeon.domain.model.QuestRelation

fun QuestRelationEntity.toDomain(): QuestRelation =
    QuestRelation(
        id = id,
        sourceQuestId = sourceQuestId,
        targetQuestId = targetQuestId,
        relationType = relationType
    )

fun QuestRelation.toEntity() =
    QuestRelationEntity(
        id = id,
        sourceQuestId = sourceQuestId,
        targetQuestId = targetQuestId,
        relationType = relationType
    )
