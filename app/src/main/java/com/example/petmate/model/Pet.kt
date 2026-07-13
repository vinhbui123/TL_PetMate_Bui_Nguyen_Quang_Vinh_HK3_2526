package com.example.petmate.model

import androidx.annotation.DrawableRes
import com.example.petmate.R

import com.google.gson.annotations.SerializedName

data class Pet(
    val id: Int,
    val name: String,
    val breed: String,
    val age: String,
    val weight: String,
    @SerializedName("gender") val sex: String,
    val distance: String,
    @SerializedName("description") val about: String,
    val imageUrl: String? = null,
    val price: String? = null,
    val status: String? = null,
    val category: String? = null,
    @DrawableRes val imageRes: Int = R.drawable.beagle_dog
)
