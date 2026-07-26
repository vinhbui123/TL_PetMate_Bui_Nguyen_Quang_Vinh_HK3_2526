package com.example.petmate.model

data class PetRequest(
    val name: String,
    val breed: String,
    val age: String,
    val weight: String,
    val gender: String,
    val description: String,
    val price: String?,
    val category: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = "AVAILABLE",
    val isVaccinated: Boolean? = false,
    val isNeutered: Boolean? = false,
    val organizationId: Long? = null
)
