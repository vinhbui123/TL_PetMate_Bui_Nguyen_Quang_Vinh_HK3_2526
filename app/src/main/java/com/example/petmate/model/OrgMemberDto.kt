package com.example.petmate.model

data class OrgMemberDto(
    val id: Long? = null,
    val userId: Long? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userAvatarUrl: String? = null,
    val memberRole: String? = null
)
