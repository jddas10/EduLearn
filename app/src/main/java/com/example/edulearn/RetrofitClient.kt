package com.example.edulearn

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://guides-faqs-mix-quite.trycloudflare.com/"

    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        sessionManager = SessionManager(context.applicationContext)
    }

    fun baseUrl(): String = BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" }

    private val authInterceptor = Interceptor { chain ->
        val token = if (::sessionManager.isInitialized) sessionManager.getToken().orEmpty() else ""
        val req = chain.request().newBuilder().apply {
            if (token.isNotBlank()) header("Authorization", "Bearer $token")
        }.build()
        chain.proceed(req)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
