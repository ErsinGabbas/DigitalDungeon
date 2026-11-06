package com.example.digitaldungeon.ui.session.components

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * Models for the session Compose tests.
 *
 * Built here rather than borrowed from the `@Preview` fixtures in main: a preview
 * exists to be looked at and is free to be rewritten for the sake of a screenshot,
 * while these values are what the assertions below are phrased against.
 *
 * [TEST_NOW] is a fixed instant for the same reason the card takes `now` as a
 * parameter — a card rendered against the wall clock would format a different
 * stamp on either side of midnight.
 */

/** 15 June 2024, 12:00 UTC. */
internal const val TEST_NOW: Long = 1_718_452_800_000L

internal val participantThalia = EventParticipant(name = "Thalia", level = 4, maxHp = 31)
internal val participantBren = EventParticipant(name = "Bren", level = 3, maxHp = 28)
internal val participantNix = EventParticipant(name = "Nix", level = 4, maxHp = 24)

internal fun testEvent(
    id: Int = 1,
    type: SessionEventType = SessionEventType.SCENE,
    title: String? = "The Sunken Gate",
    content: String = "Water drips from the arch.",
    participants: List<EventParticipant> = emptyList(),
    timestamp: Long = TEST_NOW
): SessionEvent = SessionEvent(
    id = id,
    sessionId = 1,
    type = type,
    title = title,
    content = content,
    orderIndex = id,
    timestamp = timestamp,
    participants = participants
)
