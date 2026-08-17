package com.example.petmate.model

data class SystemStatsDto(
    val totalUsers: Long,
    val totalOrganizations: Long,
    val totalPets: Long,
    val totalAdoptions: Long,
    val pendingAdoptions: Long,
    val approvedAdoptions: Long,
    val totalReports: Long,
    val adoptionTrend: List<ChartPointDto>?,
    val contentMix: List<PieChartPointDto>?
)

data class ChartPointDto(
    val label: String,
    val value: Long
)

data class PieChartPointDto(
    val label: String,
    val value: Long,
    val colorHex: String
)

data class SystemLog(
    val id: Long,
    val actionType: String,
    val actor: String,
    val description: String,
    val severity: String,
    val createdAt: String?
)
