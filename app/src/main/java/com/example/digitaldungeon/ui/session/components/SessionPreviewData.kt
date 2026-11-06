package com.example.digitaldungeon.ui.session.components

import com.example.digitaldungeon.domain.model.EventParticipant
import com.example.digitaldungeon.domain.model.Session
import com.example.digitaldungeon.domain.model.SessionEvent
import com.example.digitaldungeon.domain.model.SessionEventType

/**
 * Sample data shared by the `@Preview` functions of the session components,
 * so no preview has to build a model by hand (and none needs a ViewModel).
 */

private const val MINUTE_MILLIS: Long = 60L * 1000L
private const val HOUR_MILLIS: Long = 60L * MINUTE_MILLIS
private const val DAY_MILLIS: Long = 24L * HOUR_MILLIS

/**
 * The "now" every preview is rendered against: 15 June 2024, 12:00 UTC.
 *
 * Fixed on purpose. Timestamps below are written as offsets from it, so a card
 * renders the same stamp on every run — a preview that read the wall clock would
 * differ between two screenshots for no reason. Midday UTC also keeps the
 * same-day offsets on the same calendar day in most time zones, so the "time
 * only" tier of [formatEventTimestamp] is what the previews actually show.
 */
internal const val previewNow: Long = 1_718_452_800_000L

/**
 * The campaign roster the participant picker offers. The combat event below
 * takes its own cast from here, so picker and card agree in the previews.
 */
internal val previewCampaignParticipants: List<EventParticipant> = listOf(
    EventParticipant(name = "Thalia", level = 4, maxHp = 31),
    EventParticipant(name = "Bren", level = 3, maxHp = 28),
    EventParticipant(name = "Nix", level = 4, maxHp = 24),
    EventParticipant(name = "Orin Vale", level = 5, maxHp = 42)
)

/**
 * Someone an older event still names although the campaign no longer lists them
 * — the case the stored snapshot exists for.
 */
internal val previewDepartedParticipant: EventParticipant =
    EventParticipant(name = "Sera (left the party)", level = 2, maxHp = 17)

/** Deliberately oversized party, for checking that the picker wraps. */
internal val previewLargeParty: List<EventParticipant> =
    previewCampaignParticipants + listOf(
        EventParticipant(name = "Hendrik Stoneward", level = 6, maxHp = 55),
        EventParticipant(name = "Mira", level = 2, maxHp = 19),
        EventParticipant(name = "Ireena Kolyana", level = 4, maxHp = 28),
        EventParticipant(name = "Pip", level = 1, maxHp = 9)
    )

internal fun previewEvent(
    id: Int,
    type: SessionEventType,
    title: String?,
    content: String,
    participants: List<EventParticipant> = emptyList(),
    timestamp: Long = previewNow
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

internal val previewSceneEvent: SessionEvent = previewEvent(
    id = 1,
    type = SessionEventType.SCENE,
    title = "The Sunken Gate",
    content = "Water drips from the arch. Somewhere below, a bell rings once.",
    timestamp = previewNow - 2L * HOUR_MILLIS
)

internal val previewCombatEvent: SessionEvent = previewEvent(
    id = 2,
    type = SessionEventType.COMBAT,
    title = "Ambush at the bridge",
    content = "Three goblins and a hobgoblin captain block the crossing.",
    participants = previewCampaignParticipants.take(2),
    timestamp = previewNow - 95L * MINUTE_MILLIS
)

internal val previewDialogueEvent: SessionEvent = previewEvent(
    id = 3,
    type = SessionEventType.DIALOGUE,
    title = "Mayor Aldric",
    content = "\"The mines have been quiet for a month. That is the problem.\"",
    timestamp = previewNow - 40L * MINUTE_MILLIS
)

internal val previewDiscoveryEvent: SessionEvent = previewEvent(
    id = 4,
    type = SessionEventType.DISCOVERY,
    title = "A sealed ledger",
    content = "Bound in grey leather, the last ten pages torn out.",
    timestamp = previewNow - 10L * MINUTE_MILLIS
)

/**
 * Carried over from an earlier session, so the previews also show the second
 * formatting tier: date **and** time instead of just the time.
 */
internal val previewNoteEvent: SessionEvent = previewEvent(
    id = 5,
    type = SessionEventType.NOTE,
    title = null,
    content = "Remember: the innkeeper still owes the party 40 gold for the wolves.",
    timestamp = previewNow - 8L * DAY_MILLIS
)

/** An event whose cast includes a member the campaign has since dropped. */
internal val previewDepartedMemberEvent: SessionEvent = previewEvent(
    id = 6,
    type = SessionEventType.DIALOGUE,
    title = "A promise at the crossroads",
    content = "\"Go on without me. I will hold the pass.\"",
    participants = listOf(previewCampaignParticipants.first(), previewDepartedParticipant),
    timestamp = previewNow - 31L * DAY_MILLIS
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
    createdAt = previewNow - 3L * HOUR_MILLIS
)
