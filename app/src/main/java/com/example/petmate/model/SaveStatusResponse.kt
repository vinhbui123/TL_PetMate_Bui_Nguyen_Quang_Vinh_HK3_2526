package com.example.petmate.model

import com.google.gson.annotations.SerializedName

data class SaveStatusResponse(
    @SerializedName("saved", alternate = ["isSaved"])
    val isSaved: Boolean
)
