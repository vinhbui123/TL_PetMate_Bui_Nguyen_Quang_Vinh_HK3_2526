package com.example.petmate.model

data class PetMarketItem(
    val id: Int,
    val title: String,
    val price: String,
    val location: String,
    val timePosted: String,
    val imageRes: Int,
    val category: String = "DOGS", // e.g., "DOGS", "CATS"
    val isPriority: Boolean = false // e.g. for "Tin ưu tiên" tag
)
