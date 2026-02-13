package com.example.edulearn

data class LecturesResponse(
    val success: Boolean,
    val lectures: List<LectureModel> = emptyList(),
    val message: String? = null
)
