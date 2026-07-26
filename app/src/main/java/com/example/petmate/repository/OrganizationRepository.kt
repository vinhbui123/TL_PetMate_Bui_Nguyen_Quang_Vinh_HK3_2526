package com.example.petmate.repository

import com.example.petmate.model.*
import com.example.petmate.network.OrgApi
import okhttp3.MultipartBody
import retrofit2.Response

class OrganizationRepository(private val api: OrgApi) {
    suspend fun registerOrg(dto: OrganizationProfileDto): Response<OrganizationProfileDto> = api.registerOrg(dto)
    
    suspend fun updateOrg(id: Long, dto: OrganizationProfileDto): Response<OrganizationProfileDto> = api.updateOrg(id, dto)
    
    suspend fun getOrg(id: Long): Response<OrganizationProfileDto> = api.getOrg(id)
    
    suspend fun getMyOrg(): Response<OrganizationProfileDto> = api.getMyOrg()
    
    suspend fun getOrgByUserId(userId: Long): Response<OrganizationProfileDto> = api.getOrgByUserId(userId)
    
    suspend fun uploadLogo(id: Long, file: MultipartBody.Part): Response<OrganizationProfileDto> = api.uploadLogo(id, file)
    
    suspend fun uploadDocument(id: Long, docType: String, file: MultipartBody.Part): Response<OrgDocumentDto> = 
        api.uploadDocument(id, docType, file)
        
    suspend fun deleteDocument(id: Long, docId: Long): Response<Unit> = api.deleteDocument(id, docId)
    
    suspend fun inviteMember(id: Long, dto: InviteMemberDto): Response<OrgMemberDto> = api.inviteMember(id, dto)
    
    suspend fun removeMember(id: Long, memberId: Long): Response<Unit> = api.removeMember(id, memberId)
    
    suspend fun getMembers(id: Long): Response<List<OrgMemberDto>> = api.getMembers(id)
    
    suspend fun listOrgs(status: String? = null): Response<List<OrganizationProfileDto>> = api.listOrgs(status)
    
    suspend fun reviewOrg(id: Long, dto: OrgReviewRequest): Response<OrganizationProfileDto> = api.reviewOrg(id, dto)

    suspend fun leaveOrganization(id: Long): Response<Unit> = api.leaveOrganization(id)

    suspend fun dissolveOrganization(id: Long): Response<Unit> = api.dissolveOrganization(id)

    suspend fun transferOwnership(id: Long, newOwnerId: Long): Response<Unit> = api.transferOwnership(id, newOwnerId)
}
