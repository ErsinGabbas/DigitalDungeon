package com.example.digitaldungeon.domain.model

data class Campaign(
    val id: Int,
    val name: String,
    val description: String,
    val createdAt: Long,
    val participants: List<EventParticipant> = emptyList()
)
