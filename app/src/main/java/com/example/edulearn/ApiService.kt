package com.example.edulearn

import com.google.gson.annotations.SerializedName
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
    @SerializedName("question_text")
    val questionText: String,
    @SerializedName("opt_a")
    val optA: String,
    @SerializedName("opt_b")
    val optB: String,
    @SerializedName("opt_c")
    val optC: String,
    @SerializedName("opt_d")
    val optD: String,
    @SerializedName("correct_opt")
    val correctOpt: String,
    @SerializedName("marks")
    val marks: Int
)

data class QuizCreateRequest(
    @SerializedName("quiz_id")
    val quizId: Int? = null,
    @SerializedName("teacher_id")
    val teacherId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("total_marks")
    val totalMarks: Int,
    @SerializedName("questions")
    val questions: List<QuizQuestionPayload>
)

data class QuizCreateResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("quiz_id")
    val quizId: Int?,
    @SerializedName("message")
    val message: String?
)

data class QuizProgressRequest(
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("quiz_id")
    val quizId: Int,
    @SerializedName("current_score")
    val currentScore: Int,
    @SerializedName("status")
    val status: String = "Started"
)

data class QuizProgressResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?
)

data class QuizQuestionDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("question_text")
    val questionText: String,
    @SerializedName("opt_a")
    val optA: String,
    @SerializedName("opt_b")
    val optB: String,
    @SerializedName("opt_c")
    val optC: String,
    @SerializedName("opt_d")
    val optD: String,
    @SerializedName("correct_opt")
    val correctOpt: String,
    @SerializedName("marks")
    val marks: Int
)

data class QuizDetailResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("quiz_id")
    val quizId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("total_marks")
    val totalMarks: Int,
    @SerializedName("questions")
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
