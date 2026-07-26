package com.example.petmate.model

data class OrganizationProfileDto(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String = "",
    val address: String = "",
    val contact: String = "",
    val description: String = "",
    val logoUrl: String? = null,
    val status: String = "PENDING",
    
    val orgType: String? = null,
    val verificationLevel: String? = null,
    
    val foundedYear: Int? = null,
    val businessAddress: String? = null,
    val taxCode: String? = null,
    val establishmentNumber: String? = null,
    val website: String? = null,
    val fanpage: String? = null,
    val email: String? = null,
    val phone: String? = null,
    
    val representativeName: String? = null,
    val representativePhone: String? = null,
    val representativeEmail: String? = null,
    val representativeIdType: String? = null,
    val representativeIdNumber: String? = null,
    val representativeIdFrontUrl: String? = null,
    val representativeAvatarUrl: String? = null,
    val representativeSocialUrl: String? = null,
    val representativeRole: String? = null,
    val repLastVerifiedAt: String? = null,
    
    val documents: List<OrgDocumentDto> = emptyList(),
    
    val sterilizationPolicy: Boolean? = null,
    val vaccinationPolicy: Boolean? = null,
    val policyDescription: String? = null,
    val agreedTerms: Boolean? = null,
    
    val adminNote: String? = null,
    val rejectionReason: String? = null,
    val verifiedAt: String? = null,
    val verifiedUntil: String? = null,
    val isVerified: Boolean = false,
    val badgeLabel: String? = null,
    
    val members: List<OrgMemberDto> = emptyList(),
    val ownerName: String? = null
)
