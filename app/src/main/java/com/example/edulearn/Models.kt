package com.example.edulearn

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
    val role: String?,
    val userId: Int? = null
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

data class AttendanceStartRequest(
    val title: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int = 100,
    val accuracyM: Int = 50,
    val durationMinutes: Int = 10,
    val nonceTtlSeconds: Int = 20
)

data class AttendanceStartResponse(
    val success: Boolean,
    val sessionId: String?,
    val expiresAt: String?,
    val nonceTtlSeconds: Int?,
    val qr: String?,
    val message: String?
)

data class AttendanceNonceRequest(
    val sessionId: String,
    val ttlSeconds: Int = 20
)

data class AttendanceNonceResponse(
    val success: Boolean,
    val nonce: String?,
    val expiresAt: String?,
    val qr: String?
)

data class AttendanceCloseRequest(
    val sessionId: String
)

data class AttendanceCloseResponse(
    val success: Boolean,
    val message: String?
)

data class AttendanceMarkRequest(
    val sessionId: String,
    val nonce: String,
    val lat: Double,
    val lng: Double,
    val accuracyM: Int,
    val deviceId: String
)

data class AttendanceMarkResponse(
    val success: Boolean,
    val message: String?,
    val distanceM: Int? = null
)

data class AttendanceSessionDto(
    val id: String,
    val title: String,
    val status: String,
    val created_at: String,
    val expires_at: String,
    val radius_m: Int,
    val accuracy_m: Int
)

data class AttendanceSessionsResponse(
    val success: Boolean,
    val sessions: List<AttendanceSessionDto>?
)
