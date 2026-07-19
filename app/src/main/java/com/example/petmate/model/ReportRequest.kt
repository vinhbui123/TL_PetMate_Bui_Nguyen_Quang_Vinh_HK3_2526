package com.example.petmate.model

data class ReportRequest(
    val reportedPetId: Long? = null,
    val reportedUserId: Long? = null,
    val reason: String,
    val description: String? = null
)
