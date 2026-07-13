package com.example.petmate.model

data class User(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String, // MEMBER, RESCUE_ORG, ADMIN
    val phone: String? = null,
    val address: String? = null,
    val avatarUrl: String? = null,
    val status: String? = null
)
