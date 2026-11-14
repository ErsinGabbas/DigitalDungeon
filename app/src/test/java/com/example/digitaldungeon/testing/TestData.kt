package com.example.digitaldungeon.testing

import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Quest
import com.example.digitaldungeon.domain.model.QuestCategory
import com.example.digitaldungeon.domain.model.QuestObjective
import com.example.digitaldungeon.domain.model.QuestObjectiveStatus
import com.example.digitaldungeon.domain.model.QuestPriority
import com.example.digitaldungeon.domain.model.QuestStatus
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * Builders for the domain models, so a test only spells out the fields it actually
 * cares about and stays readable when a model grows another column.
 */

fun campaign(
    id: Int = 1,
    name: String = "Campaign $id",
    description: String = "description of campaign $id",
    createdAt: Long = 1_000L,
    participants: List<EventParticipant> = emptyList()
) = Campaign(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
    participants = participants
)

fun session(
    id: Int = 1,
    campaignId: Int = 1,
    title: String = "Session $id",
    notes: String = "notes of session $id",
    createdAt: Long = 1_000L
) = Session(
    id = id,
    campaignId = campaignId,
    title = title,
    notes = notes,
    createdAt = createdAt
)

fun participant(
    name: String = "Hero",
    level: Int = 3,
    maxHp: Int = 25
) = EventParticipant(
    name = name,
    level = level,
    maxHp = maxHp
)

fun quest(
    id: Int = 1,
    campaignId: Int = 1,
    title: String = "Quest $id",
    description: String = "description of quest $id",
    category: QuestCategory = QuestCategory.SIDE,
    status: QuestStatus = QuestStatus.NOT_STARTED,
    priority: QuestPriority = QuestPriority.NORMAL,
    isHidden: Boolean = false,
    createdAt: Long = 1_000L,
    inGameDate: String? = null
) = Quest(
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

fun questObjective(
    id: Int = 1,
    questId: Int = 1,
    title: String = "Objective $id",
    description: String = "details of objective $id",
    status: QuestObjectiveStatus = QuestObjectiveStatus.NOT_STARTED,
    orderIndex: Int = 0,
    isOptional: Boolean = false
) = QuestObjective(
    id = id,
    questId = questId,
    title = title,
    description = description,
    status = status,
    orderIndex = orderIndex,
    isOptional = isOptional
)

fun sessionEvent(
    id: Int = 1,
    sessionId: Int = 1,
    type: SessionEventType = SessionEventType.NOTE,
    title: String? = null,
    content: String = "content of event $id",
    orderIndex: Int = 0,
    timestamp: Long = 1_000L,
    participants: List<EventParticipant> = emptyList()
) = SessionEvent(
    id = id,
    sessionId = sessionId,
    type = type,
    title = title,
    content = content,
    orderIndex = orderIndex,
    timestamp = timestamp,
    participants = participants
)
