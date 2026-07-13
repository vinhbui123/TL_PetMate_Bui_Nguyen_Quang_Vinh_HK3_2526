package com.example.petmate.model

data class PetRequestDto(
    val name: String,
    val breed: String,
    val age: String,
    val weight: String,
    val gender: String,
    val distance: String,
    val description: String,
    val price: String?,
    val category: String,
    val status: String? = "AVAILABLE"
)
