package com.example.petmate.model

data class RedListSpecies(
    val id: Long = 0,
    val category: String? = null,
    val breedKeyword: String = "",
    val synonyms: String? = null,
    val protectionLevel: String = "RESTRICTED",
    val description: String? = null,
    val createdAt: String? = null
)

data class RedListRequest(
    val category: String? = null,
    val breedKeyword: String = "",
    val synonyms: String? = null,
    val protectionLevel: String = "RESTRICTED",
    val description: String? = null
)

data class RedListCheckResult(
    val matched: Boolean = false,
    val species: RedListSpecies? = null,
    val matchedKeyword: String? = null,
    val matchType: String? = null
)
