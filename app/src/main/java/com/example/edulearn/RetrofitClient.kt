package com.example.edulearn

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    const val BASE_URL = "https://len-ontario-experiments-painting.trycloudflare.com/"

    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    fun baseUrl(): String = BASE_URL.trim()

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = if (::sessionManager.isInitialized) sessionManager.getToken().orEmpty() else ""

        val newRequest = if (token.isNotBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else request

        chain.proceed(newRequest)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL.trim())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
