package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestHistoryEntity
import com.example.digitaldungeon.domain.model.QuestHistoryEntry

fun QuestHistoryEntity.toDomain(): QuestHistoryEntry =
    QuestHistoryEntry(
        id = id,
        questId = questId,
        eventType = eventType,
        description = description,
        timestamp = timestamp
    )

fun QuestHistoryEntry.toEntity() =
    QuestHistoryEntity(
        id = id,
        questId = questId,
        eventType = eventType,
        description = description,
        timestamp = timestamp
    )
