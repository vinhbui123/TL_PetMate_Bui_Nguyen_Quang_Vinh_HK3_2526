package com.example.petmate.model

data class SystemStatsDto(
    val totalUsers: Long,
    val totalOrganizations: Long,
    val totalPets: Long,
    val totalAdoptions: Long,
    val pendingAdoptions: Long,
    val approvedAdoptions: Long,
    val totalReports: Long
)

data class SystemLog(
    val id: Long,
    val actionType: String,
    val actor: String,
    val description: String,
    val severity: String,
    val createdAt: String?
)
