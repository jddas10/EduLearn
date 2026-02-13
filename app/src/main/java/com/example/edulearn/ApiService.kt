package com.example.edulearn

import com.example.edulearn.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ===== AUTH =====
    @POST("auth/login")
    fun loginUser(@Body request: LoginRequest): Call<AuthResponse>

    // ===== QUIZ =====
    @POST("api/quiz/create")
    fun createQuiz(@Body request: QuizCreateRequest): Call<QuizCreateResponse>

    @POST("api/quiz/sync-progress")
    fun syncQuizProgress(@Body request: QuizProgressRequest): Call<QuizProgressResponse>

    @GET("api/quiz/{quizId}")
    fun getQuiz(@Path("quizId") quizId: Int): Call<QuizDetailResponse>

    // ===== LECTURES =====
    @GET("lectures")
    fun getLectures(): Call<LecturesResponse>

    @POST("bookmarks/toggle")
    fun toggleBookmark(@Body req: ToggleBookmarkRequest): Call<ToggleBookmarkResponse>

    @Multipart
    @POST("lectures/upload")
    fun uploadLecture(
        @Part video: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("subject") subject: RequestBody,
        @Part("category") category: RequestBody
    ): Call<Any>

    @DELETE("lectures/{id}")
    fun deleteLecture(@Path("id") id: Int): Call<SimpleResponse>

    // ===== ATTENDANCE =====
    @POST("attendance/start")
    fun attendanceStart(
        @Header("Authorization") bearer: String,
        @Body req: AttendanceStartRequest
    ): Call<AttendanceStartResponse>

    @POST("attendance/nonce")
    fun attendanceNonce(
        @Header("Authorization") bearer: String,
        @Body req: AttendanceNonceRequest
    ): Call<AttendanceNonceResponse>

    @POST("attendance/close")
    fun attendanceClose(
        @Header("Authorization") bearer: String,
        @Body req: AttendanceCloseRequest
    ): Call<AttendanceCloseResponse>

    @POST("attendance/mark")
    fun attendanceMark(
        @Header("Authorization") bearer: String,
        @Body req: AttendanceMarkRequest
    ): Call<AttendanceMarkResponse>

    @GET("attendance/sessions")
    fun attendanceSessions(
        @Header("Authorization") bearer: String,
        @Query("date") date: String
    ): Call<AttendanceSessionsResponse>
}
