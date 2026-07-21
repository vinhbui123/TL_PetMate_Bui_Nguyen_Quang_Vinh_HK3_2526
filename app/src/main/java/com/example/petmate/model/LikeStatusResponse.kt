package com.example.petmate.model

import com.google.gson.annotations.SerializedName

data class LikeStatusResponse(
    @SerializedName("liked")
    val liked: Boolean,
    
    @SerializedName("likeCount")
    val likeCount: Int
)
