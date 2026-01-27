package com.example.edulearn

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


data class LoginRequest(val email: String, val password: String)
data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val role: String?,
    val message: String?
)

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)

interface ApiService {

    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>

    @POST("auth/signup")
    fun registerUser(@Body request: SignupRequest): Call<AuthResponse>
}