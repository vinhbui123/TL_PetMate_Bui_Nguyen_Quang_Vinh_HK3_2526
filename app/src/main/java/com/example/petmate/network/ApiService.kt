package com.example.petmate.network

import com.example.petmate.model.AdoptionRequest
import com.example.petmate.model.AdoptionResponse
import com.example.petmate.model.ChatRoom
import com.example.petmate.model.ChatbotRequest
import com.example.petmate.model.ChatbotResponse
import com.example.petmate.model.Message
import com.example.petmate.model.Pet
import com.example.petmate.model.PetRequest
import com.example.petmate.model.ReportRequest
import com.example.petmate.model.RatingRequest
import com.example.petmate.model.RatingResponse
import com.example.petmate.model.SellerRatingSummary
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import com.example.petmate.model.User
import retrofit2.Response

interface ApiService {
    @GET("pets")
    suspend fun getPets(@Query("category") category: String? = null): List<Pet>

    @GET("pets/{id}")
    suspend fun getPetById(@Path("id") id: Int): Pet

    @POST("user/sync")
    suspend fun syncUser(@Body body: Map<String, String?>): User

    @POST("user/fcm-token")
    suspend fun registerFcmToken(@Body body: Map<String, String?>): Response<Unit>

    @DELETE("user/fcm-token")
    suspend fun removeFcmToken(@Query("token") token: String): Response<Unit>

    @DELETE("user/fcm-token/all")
    suspend fun removeAllFcmTokens(): Response<Unit>

    @GET("user/me")
    suspend fun getProfile(): User

    @PUT("user/me")
    suspend fun updateProfile(@Body user: User): User

    @Multipart
    @POST("user/avatar")
    suspend fun uploadAvatar(@Part image: MultipartBody.Part): User

    @GET("pets/my-pets")
    suspend fun getMyPets(): List<Pet>

    @GET("pets/org/{orgId}")
    suspend fun getOrgPets(@Path("orgId") orgId: Long): List<Pet>

    @GET("pets/user/{userId}")
    suspend fun getPetsByUser(@Path("userId") userId: Long): List<Pet>

    @POST("pets")
    suspend fun createPet(@Body petDto: PetRequest): Pet

    @PUT("pets/{id}")
    suspend fun updatePet(@Path("id") id: Int, @Body petDto: PetRequest): Pet

    @GET("pets/pending")
    suspend fun getPendingPets(): List<Pet>

    @GET("pets/admin/all")
    suspend fun getAllPetsAdmin(): List<Pet>

    @PUT("pets/{id}/status")
    suspend fun updatePetStatus(@Path("id") id: Int, @Query("status") status: String, @Body empty: Any = Any()): Pet

    @DELETE("pets/{id}")
    suspend fun deletePet(@Path("id") id: Int)

    @POST("pets/{id}/mark-sold")
    suspend fun markPetAsSold(@Path("id") id: Int, @Query("buyerId") buyerId: Long): Response<Unit>

    @Multipart
    @POST("pets/{id}/image")
    suspend fun uploadPetImage(@Path("id") id: Int, @Part image: MultipartBody.Part): Pet

    @POST("pets/check-red-list")
    suspend fun checkRedList(@Body petDto: PetRequest): com.example.petmate.model.RedListCheckResult

    @PUT("user/location")
    suspend fun updateLocation(@Body body: Map<String, Double>): User

    @POST("user/change-password")
    suspend fun changePassword(@Body request: com.example.petmate.model.ChangePasswordRequest): Response<Unit>

    @DELETE("user/account")
    suspend fun deleteAccount(): Response<Unit>

    @POST("user/blocks/{blockedId}")
    suspend fun blockUser(@Path("blockedId") blockedId: Long, @Body empty: Any = Any()): Response<Unit>

    @DELETE("user/blocks/{blockedId}")
    suspend fun unblockUser(@Path("blockedId") blockedId: Long): Response<Unit>

    @GET("user/blocks")
    suspend fun getBlockedUsers(): List<Long>

    @GET("user/blocks/details")
    suspend fun getBlockedUserDetails(): List<User>

    @POST("user/follows/{followedId}")
    suspend fun followUser(@Path("followedId") followedId: Long, @Body empty: Any = Any()): Response<Unit>

    @DELETE("user/follows/{followedId}")
    suspend fun unfollowUser(@Path("followedId") followedId: Long): Response<Unit>

    @GET("user/follows/status/{followedId}")
    suspend fun checkFollowStatus(@Path("followedId") followedId: Long): Boolean

    @GET("user/follows/{userId}/stats")
    suspend fun getUserFollowStats(@Path("userId") userId: Long): Map<String, Long>

    @GET("user/follows/{userId}/followers")
    suspend fun getFollowers(@Path("userId") userId: Long): List<User>

    @GET("user/follows/{userId}/following")
    suspend fun getFollowing(@Path("userId") userId: Long): List<User>

    @POST("reports")
    suspend fun submitReport(@Body request: ReportRequest): Response<Unit>

    @GET("reports")
    suspend fun getAllReports(): List<com.example.petmate.model.ReportResponse>

    @PUT("reports/{id}/status")
    suspend fun updateReportStatus(
        @Path("id") id: Long,
        @Query("status") status: String,
        @Body empty: Any = Any()
    ): com.example.petmate.model.ReportResponse

    @GET("chat/rooms")
    suspend fun getChatRooms(@Query("userId") userId: Long): List<ChatRoom>

    @GET("chat/rooms/{roomId}/messages")
    suspend fun getChatMessages(@Path("roomId") roomId: Long): List<Message>

    @POST("chat/rooms/start")
    suspend fun getOrCreateRoom(@Body payload: Map<String, Long>): ChatRoom

    @PUT("chat/rooms/{roomId}/read")
    suspend fun markRoomAsRead(@Path("roomId") roomId: Long, @Query("userId") userId: Long, @Body empty: Any = Any())

    @GET("chat/unread-count")
    suspend fun getTotalUnreadCount(@Query("userId") userId: Long): Int

    @GET("chat/pet/{petId}/buyers")
    suspend fun getBuyersForPet(@Path("petId") petId: Int): List<User>

    @POST("chatbot/ask")
    suspend fun askChatbot(@Body request: ChatbotRequest): ChatbotResponse

    @POST("adoptions/apply")
    suspend fun applyForAdoption(@Body request: AdoptionRequest): AdoptionResponse

    @GET("adoptions/my-applications")
    suspend fun getMyAdoptionApplications(): List<AdoptionResponse>

    @GET("adoptions/check-approved/{petId}")
    suspend fun checkApprovedAdoption(@Path("petId") petId: Long): Boolean

    @GET("adoptions/received")
    suspend fun getReceivedAdoptionApplications(): List<AdoptionResponse>

    @GET("adoptions/org/{orgId}")
    suspend fun getOrgAdoptions(@Path("orgId") orgId: Long): List<AdoptionResponse>

    @PUT("adoptions/{id}/status")
    suspend fun updateAdoptionStatus(
        @Path("id") id: Long,
        @Query("status") status: String,
        @Body empty: Any = Any()
    ): AdoptionResponse

    @DELETE("adoptions/{id}")
    suspend fun cancelAdoptionApplication(@Path("id") id: Long): retrofit2.Response<Unit>

    @GET("pets/saved")
    suspend fun getSavedPets(): List<Pet>

    // Likes
    @GET("pets/{petId}/like-status")
    suspend fun getLikeStatus(@Path("petId") petId: Long): com.example.petmate.model.LikeStatusResponse

    @POST("pets/{petId}/like")
    suspend fun toggleLike(@Path("petId") petId: Long, @Body empty: Any = Any()): com.example.petmate.model.LikeStatusResponse

    // Interactions
    @GET("pets/{petId}/save-status")
    suspend fun getSaveStatus(@Path("petId") petId: Long): com.example.petmate.model.SaveStatusResponse

    @POST("pets/{petId}/save")
    suspend fun toggleSave(@Path("petId") petId: Long, @Body empty: Any = Any()): com.example.petmate.model.SaveStatusResponse

    // Admin Endpoints
    @GET("admin/stats")
    suspend fun getSystemStats(): com.example.petmate.model.SystemStatsDto

    @GET("admin/logs")
    suspend fun getSystemLogs(): List<com.example.petmate.model.SystemLog>

    @GET("admin/users/pending-rescue")
    suspend fun getPendingRescueOrgs(): List<User>

    @PUT("admin/users/{id}/approve-rescue")
    suspend fun approveRescueOrg(
        @Path("id") id: Long,
        @Query("approve") approve: Boolean,
        @Body empty: Any = Any()
    ): User

    @POST("admin/broadcast")
    suspend fun sendSystemBroadcast(@Body payload: Map<String, String>): Response<Unit>

    @GET("admin/users")
    suspend fun getAllUsersAdmin(): List<User>

    @PUT("admin/users/{id}/status")
    suspend fun updateUserStatusAdmin(
        @Path("id") id: Long,
        @Query("status") status: String,
        @Body empty: Any = Any()
    ): User

    @PUT("admin/users/{id}/role")
    suspend fun updateUserRoleAdmin(
        @Path("id") id: Long,
        @Query("role") role: String,
        @Body empty: Any = Any()
    ): User

    @GET("user/{userId}/ratings/summary")
    suspend fun getSellerRatingSummary(@Path("userId") userId: Long): SellerRatingSummary

    @POST("user/{userId}/rate")
    suspend fun rateUser(@Path("userId") userId: Long, @Body request: RatingRequest): RatingResponse

    @DELETE("user/ratings/{ratingId}")
    suspend fun deleteRating(@Path("ratingId") ratingId: Long): Response<Unit>

    @GET("admin/red-list")
    suspend fun getRedList(): List<com.example.petmate.model.RedListSpecies>

    @POST("admin/red-list")
    suspend fun addRedListSpecies(@Body req: com.example.petmate.model.RedListRequest): com.example.petmate.model.RedListSpecies

    @DELETE("admin/red-list/{id}")
    suspend fun removeRedListSpecies(@Path("id") id: Long): Response<Unit>

    @GET("admin/red-list/pending")
    suspend fun getRedListPendingPets(): List<Pet>

    @PUT("admin/red-list/pets/{petId}/approve")
    suspend fun approveRedListPet(@Path("petId") petId: Int): Pet

    @PUT("admin/red-list/pets/{petId}/reject")
    suspend fun rejectRedListPet(@Path("petId") petId: Int): Pet

    @PUT("admin/red-list/pets/{petId}/unlock")
    suspend fun unlockRedListPet(@Path("petId") petId: Int): Pet

    @GET("pets/pending-red-list")
    suspend fun getPendingRedListPets(): List<Pet>

    @PUT("user/request-rescue-org")
    suspend fun requestRescueOrg(): User

    @GET("user/{userId}/ratings")
    suspend fun getSellerReviews(@Path("userId") userId: Long): List<RatingResponse>

    @GET("user/{userId}/rating-status")
    suspend fun checkRatingStatus(@Path("userId") userId: Long): Boolean
}
