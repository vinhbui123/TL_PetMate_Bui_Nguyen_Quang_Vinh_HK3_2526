package com.example.petmate.model

data class OrgReviewRequest(
    val status: String,
    val adminNote: String? = null,
    val rejectionReason: String? = null
)
