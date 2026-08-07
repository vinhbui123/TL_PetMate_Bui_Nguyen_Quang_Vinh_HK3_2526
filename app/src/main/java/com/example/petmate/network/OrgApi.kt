package com.example.petmate.network

import com.example.petmate.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface OrgApi {
    @POST("/api/orgs")
    suspend fun registerOrg(@Body dto: OrganizationProfileDto): Response<OrganizationProfileDto>

    @PUT("/api/orgs/{id}")
    suspend fun updateOrg(@Path("id") id: Long, @Body dto: OrganizationProfileDto): Response<OrganizationProfileDto>

    @GET("/api/orgs/{id}")
    suspend fun getOrg(@Path("id") id: Long): Response<OrganizationProfileDto>

    @GET("/api/orgs/my")
    suspend fun getMyOrg(): Response<OrganizationProfileDto>

    @GET("/api/orgs/user/{userId}")
    suspend fun getOrgByUserId(@Path("userId") userId: Long): Response<OrganizationProfileDto>

    @Multipart
    @POST("/api/orgs/{id}/logo")
    suspend fun uploadLogo(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part
    ): Response<OrganizationProfileDto>

    @Multipart
    @POST("/api/orgs/{id}/documents")
    suspend fun uploadDocument(
        @Path("id") id: Long,
        @Query("docType") docType: String,
        @Part file: MultipartBody.Part
    ): Response<OrgDocumentDto>

    @DELETE("/api/orgs/{id}/documents/{docId}")
    suspend fun deleteDocument(@Path("id") id: Long, @Path("docId") docId: Long): Response<Unit>

    @POST("/api/orgs/{id}/members")
    suspend fun inviteMember(@Path("id") id: Long, @Body dto: InviteMemberDto): Response<OrgMemberDto>

    @DELETE("/api/orgs/{id}/members/{memberId}")
    suspend fun removeMember(@Path("id") id: Long, @Path("memberId") memberId: Long): Response<Unit>

    @POST("/api/orgs/{id}/members/{memberId}/accept")
    suspend fun acceptInvitation(@Path("id") id: Long, @Path("memberId") memberId: Long): Response<OrgMemberDto>

    @POST("/api/orgs/{id}/members/{memberId}/reject")
    suspend fun rejectInvitation(@Path("id") id: Long, @Path("memberId") memberId: Long): Response<Unit>

    @GET("/api/orgs/{id}/members")
    suspend fun getMembers(@Path("id") id: Long): Response<List<OrgMemberDto>>

    @GET("/api/orgs")
    suspend fun listOrgs(@Query("status") status: String? = null): Response<List<OrganizationProfileDto>>

    @PUT("/api/orgs/{id}/review")
    suspend fun reviewOrg(@Path("id") id: Long, @Body dto: OrgReviewRequest): Response<OrganizationProfileDto>

    @DELETE("/api/orgs/{id}/leave")
    suspend fun leaveOrganization(@Path("id") id: Long): Response<Unit>

    @DELETE("/api/orgs/{id}")
    suspend fun dissolveOrganization(@Path("id") id: Long): Response<Unit>

    @PUT("/api/orgs/{id}/transfer-ownership/{newOwnerId}")
    suspend fun transferOwnership(@Path("id") id: Long, @Path("newOwnerId") newOwnerId: Long): Response<Unit>
}
