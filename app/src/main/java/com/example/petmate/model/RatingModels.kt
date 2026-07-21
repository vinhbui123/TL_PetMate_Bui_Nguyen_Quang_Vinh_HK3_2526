package com.example.petmate.model

data class RatingRequest(
    val score: Double,
    val petId: Long,
    val comment: String?
)

data class RatingResponse(
    val id: Long,
    val raterId: Long,
    val raterName: String,
    val raterAvatarUrl: String?,
    val score: Double,
    val comment: String?,
    val petId: Long?,
    val petName: String?,
    val petPrice: String?,
    val petImageUrl: String?,
    val createdAt: String
)

data class SellerRatingSummary(
    val sellerId: Long,
    val sellerName: String,
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<String, Int>,
    val currentUserHasRated: Boolean,
    val currentUserRating: RatingResponse?,
    val recentReviews: List<RatingResponse>
)
