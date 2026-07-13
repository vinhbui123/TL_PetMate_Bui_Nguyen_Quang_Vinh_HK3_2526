package com.example.petmate.network

import com.example.petmate.model.Pet
import retrofit2.http.GET
import retrofit2.http.Query

import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody
import com.example.petmate.model.User

interface ApiService {
    @GET("pets")
    suspend fun getPets(@Query("category") category: String? = null): List<Pet>
    
    @POST("user/sync")
    suspend fun syncUser(@Body body: Map<String, String?>): User
    
    @GET("user/me")
    suspend fun getProfile(): User
    
    @PUT("user/me")
    suspend fun updateProfile(@Body user: User): User
    
    @Multipart
    @POST("user/avatar")
    suspend fun uploadAvatar(@Part image: MultipartBody.Part): User
    
    @GET("pets/my-pets")
    suspend fun getMyPets(): List<Pet>
    
    @POST("pets")
    suspend fun createPet(@Body petDto: com.example.petmate.model.PetRequestDto): Pet
    
    @PUT("pets/{id}")
    suspend fun updatePet(@retrofit2.http.Path("id") id: Int, @Body petDto: com.example.petmate.model.PetRequestDto): Pet
    
    @retrofit2.http.DELETE("pets/{id}")
    suspend fun deletePet(@retrofit2.http.Path("id") id: Int)
    
    @Multipart
    @POST("pets/{id}/image")
    suspend fun uploadPetImage(@retrofit2.http.Path("id") id: Int, @Part image: MultipartBody.Part): Pet
}
