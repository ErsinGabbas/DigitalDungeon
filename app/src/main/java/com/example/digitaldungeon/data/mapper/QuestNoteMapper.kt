package com.example.digitaldungeon.data.mapper

import com.example.digitaldungeon.data.local.entity.QuestNoteEntity
import com.example.digitaldungeon.domain.model.QuestNote

fun QuestNoteEntity.toDomain(): QuestNote =
    QuestNote(
        id = id,
        questId = questId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun QuestNote.toEntity() =
    QuestNoteEntity(
        id = id,
        questId = questId,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
