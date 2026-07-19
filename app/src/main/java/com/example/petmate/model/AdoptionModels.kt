package com.example.petmate.model

data class AdoptionRequest(
    val petId: Long,
    val message: String,
    val experience: String,
    val phone: String? = null,
    val houseType: String? = null,
    val hasFencedYard: Boolean? = null,
    val hasOtherPets: Boolean? = null,
    val otherPetsDetails: String? = null,
    val job: String? = null
)

data class AdoptionResponse(
    val id: Long,
    val petId: Long,
    val petName: String,
    val petImageUrl: String?,
    val applicantId: Long,
    val applicantName: String,
    val applicantAvatarUrl: String?,
    val applicantPhone: String?,
    val message: String,
    val experience: String,
    val status: String,
    val createdAt: String
)
