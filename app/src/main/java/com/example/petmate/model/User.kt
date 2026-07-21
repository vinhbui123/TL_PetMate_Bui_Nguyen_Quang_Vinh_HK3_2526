package com.example.petmate.model

import androidx.compose.runtime.Immutable

@Immutable
data class User(
    val id: Long,
    val providerId: String? = null,
    val email: String,
    val fullName: String,
    val role: String, // MEMBER, RESCUE_ORG, ADMIN
    val phone: String? = null,
    val address: String? = null,
    val avatarUrl: String? = null,
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageRating: Double? = 0.0,
    val ratingCount: Int? = 0,
    val lastActiveAt: String? = null
)
