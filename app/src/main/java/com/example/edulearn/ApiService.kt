package com.example.edulearn

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// ✅ Login request (username + password + role)
data class LoginRequest(
    val username: String,
    val password: String,
    val role: String
)

// ✅ Backend response (supports both old + new)
data class AuthResponse(
    val success: Boolean? = null,
    val token: String? = null,
    val message: String? = null,
    val name: String? = null,
    val role: String? = null,
    val user: User? = null
)

data class ApiUser(
    val id: Int? = null,
    val username: String? = null,
    val role: String? = null
)

// अगर signup use karna hai to email wala rakh sakte ho,
// but tum bole email nahi chahiye, so username based signup:
data class SignupRequest(
    val username: String,
    val name: String? = null,
    val password: String,
    val role: String
)

interface ApiService {

    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>

    @POST("auth/signup")
    fun registerUser(@Body request: SignupRequest): Call<AuthResponse>
}
