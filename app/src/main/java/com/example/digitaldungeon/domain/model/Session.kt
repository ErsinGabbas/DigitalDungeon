package com.example.digitaldungeon.domain.model

data class Session(
    val id: Int,
    val campaignId: Int,
    val title: String,
    val notes: String,
    val createdAt: Long
)
