package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestSessionLinkEntity
import com.example.digitaldungeon.domain.model.QuestSessionLink

fun QuestSessionLinkEntity.toDomain(): QuestSessionLink =
    QuestSessionLink(
        id = id,
        questId = questId,
        sessionId = sessionId
    )

fun QuestSessionLink.toEntity() =
    QuestSessionLinkEntity(
        id = id,
        questId = questId,
        sessionId = sessionId
    )
