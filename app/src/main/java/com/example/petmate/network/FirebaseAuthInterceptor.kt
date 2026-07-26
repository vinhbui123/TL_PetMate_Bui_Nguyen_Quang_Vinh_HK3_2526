package com.example.petmate.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class FirebaseAuthInterceptor(private val tokenProvider: suspend () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider() } ?: ""
        val req = chain.request().newBuilder()
            .apply { if (token.isNotEmpty()) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(req)
    }
}
