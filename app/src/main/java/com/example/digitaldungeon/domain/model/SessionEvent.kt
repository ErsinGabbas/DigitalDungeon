package com.example.digitaldungeon.domain.model

data class SessionEvent(
    val id: Int,
    val sessionId: Int,
    val type: SessionEventType,
    val title: String?,
    val content: String,
    val orderIndex: Int,
    val timestamp: Long,
    val participants: List<EventParticipant> = emptyList()
)

enum class SessionEventType {
    SCENE,
    COMBAT,
    DIALOGUE,
    DISCOVERY,
    NOTE
}
