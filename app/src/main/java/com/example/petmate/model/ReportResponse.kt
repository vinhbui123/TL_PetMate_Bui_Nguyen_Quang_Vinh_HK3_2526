package com.example.petmate.model

data class ReportResponse(
    val id: Long,
    val reporter: User,
    val reportedPet: Pet?,
    val reportedUser: User?,
    val reportedMessage: Message?,
    val reason: String,
    val description: String?,
    val status: String,
    val createdAt: String
)
