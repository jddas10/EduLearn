package com.example.edulearn

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

data class QuizQuestionPayload(
    val questionText: String,
    val optA: String,
    val optB: String,
    val optC: String,
    val optD: String,
    val correctOpt: String,
    val marks: Int
)

data class QuizCreateRequest(
    val quizId: Int? = null,
    val teacherId: Int,
    val title: String,
    val totalMarks: Int,
    val questions: List<QuizQuestionPayload>
)

data class QuizCreateResponse(
    val success: Boolean,
    val quizId: Int?,
    val message: String?
)

data class QuizProgressRequest(
    val studentId: Int,
    val quizId: Int,
    val currentScore: Int,
    val status: String = "Started"
)

data class QuizProgressResponse(
    val success: Boolean,
    val message: String?
)

data class QuizQuestionDto(
    val id: Int,
    val questionText: String,
    val optA: String,
    val optB: String,
    val optC: String,
    val optD: String,
    val correctOpt: String,
    val marks: Int
)

data class QuizDetailResponse(
    val success: Boolean,
    val quizId: Int,
    val title: String,
    val totalMarks: Int,
    val questions: List<QuizQuestionDto>
)

interface ApiService {
    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>

    @POST("quiz/create")
    fun createQuiz(@Body request: QuizCreateRequest): Call<QuizCreateResponse>

    @POST("quiz/sync-progress")
    fun syncQuizProgress(@Body request: QuizProgressRequest): Call<QuizProgressResponse>

    @GET("quiz/{quizId}")
    fun getQuiz(@Path("quizId") quizId: Int): Call<QuizDetailResponse>
}
