package com.example.edulearn

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val username: String,
    val password: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val name: String?,
    val role: String?
)

interface ApiService {
    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>
}