package com.example.petmate.network

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val BASE_URL = "https://your-server.example.com" // set to your server

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor { Firebase.auth.currentUser?.getIdToken(false)?.await()?.token })
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val orgApi: OrgApi by lazy { retrofit.create(OrgApi::class.java) }
}
