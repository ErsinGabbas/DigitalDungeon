package com.example.digitaldungeon.ui.session.components

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * Sample data shared by the `@Preview` functions of the session components,
 * so no preview has to build a model by hand (and none needs a ViewModel).
 */

internal fun previewEvent(
    id: Int,
    type: SessionEventType,
    title: String?,
    content: String,
    participants: List<EventParticipant> = emptyList()
): SessionEvent = SessionEvent(
    id = id,
    sessionId = 1,
    type = type,
    title = title,
    content = content,
    orderIndex = id,
    timestamp = 0L,
    participants = participants
)

internal val previewSceneEvent: SessionEvent = previewEvent(
    id = 1,
    type = SessionEventType.SCENE,
    title = "The Sunken Gate",
    content = "Water drips from the arch. Somewhere below, a bell rings once."
)

internal val previewCombatEvent: SessionEvent = previewEvent(
    id = 2,
    type = SessionEventType.COMBAT,
    title = "Ambush at the bridge",
    content = "Three goblins and a hobgoblin captain block the crossing.",
    participants = listOf(
        EventParticipant(name = "Thalia", level = 4, maxHp = 31),
        EventParticipant(name = "Bren", level = 3, maxHp = 28)
    )
)

internal val previewDialogueEvent: SessionEvent = previewEvent(
    id = 3,
    type = SessionEventType.DIALOGUE,
    title = "Mayor Aldric",
    content = "\"The mines have been quiet for a month. That is the problem.\""
)

internal val previewDiscoveryEvent: SessionEvent = previewEvent(
    id = 4,
    type = SessionEventType.DISCOVERY,
    title = "A sealed ledger",
    content = "Bound in grey leather, the last ten pages torn out."
)

internal val previewNoteEvent: SessionEvent = previewEvent(
    id = 5,
    type = SessionEventType.NOTE,
    title = null,
    content = "Remember: the innkeeper still owes the party 40 gold for the wolves."
)

internal val previewAllEvents: List<SessionEvent> = listOf(
    previewSceneEvent,
    previewCombatEvent,
    previewDialogueEvent,
    previewDiscoveryEvent,
    previewNoteEvent
)

internal val previewSession: Session = Session(
    id = 1,
    campaignId = 1,
    title = "Session 4 - Under the Sunken Gate",
    notes = "The party finally reaches the flooded undercity.",
    createdAt = 0L
)
