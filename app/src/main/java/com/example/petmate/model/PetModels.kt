package com.example.petmate.model

import androidx.annotation.DrawableRes
import com.example.petmate.R

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class PetUser(
    val id: Long? = null,
    val providerId: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val avatarUrl: String? = null,
    val role: String? = null,
    val status: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageRating: Double? = 0.0,
    val trustScore: Double? = null,
    val ratingCount: Int? = 0,    
    val violationCount: Int? = 0,    
    val lastActiveAt: String? = null
)

@Immutable
data class Pet(
    val id: Int,
    val name: String? = null,
    val breed: String? = null,
    @SerializedName("ageMonths") val age: String? = null,
    val weight: String? = null,
    @SerializedName("gender") val sex: String? = null,
    @SerializedName("description") val about: String? = null,
    val imageUrl: String? = null,
    val price: String? = null,
    val status: String? = null,
    val category: String? = null,
    val user: PetUser? = null,
    val likeCount: Int = 0,
    val createdAt: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isVaccinated: Boolean = false,
    val isNeutered: Boolean = false,
    val averageRating: Double? = 0.0,
    val ratingCount: Int? = 0,
    val organization: OrganizationProfileDto? = null,
    @DrawableRes val imageRes: Int = R.drawable.beagle_dog
)
