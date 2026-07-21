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
    val ratingCount: Int? = 0,
    val lastActiveAt: String? = null
)

@Immutable
data class Pet(
    val id: Int,
    val name: String,
    val breed: String,
    val age: String,
    val weight: String,
    @SerializedName("gender") val sex: String,
    @SerializedName("description") val about: String,
    val imageUrl: String? = null,
    val price: String? = null,
    val status: String? = null,
    val category: String? = null,
    val user: PetUser? = null,
    val likeCount: Int = 0,
    val createdAt: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageRating: Double? = 0.0,
    val ratingCount: Int? = 0,
    @DrawableRes val imageRes: Int = R.drawable.beagle_dog
)
