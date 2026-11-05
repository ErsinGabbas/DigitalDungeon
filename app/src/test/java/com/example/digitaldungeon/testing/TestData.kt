package com.example.digitaldungeon.testing

import com.example.digitaldungeon.domain.model.Campaign
import com.example.digitaldungeon.domain.model.EventParticipant
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
