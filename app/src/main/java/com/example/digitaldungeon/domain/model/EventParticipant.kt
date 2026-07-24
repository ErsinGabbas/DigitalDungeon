package com.example.digitaldungeon.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of a character at event creation time.
 * Contains only essential display information - no combat state or additional attributes.
 */
@Serializable
data class EventParticipant(
    val name: String,
    val level: Int,
    val maxHp: Int
)
