package com.example.edulearn.model

data class LectureModel(
    val id: Int,
    val teacherId: Int,
    val title: String,
    val subject: String,
    val category: String,
    val createdAt: String,
    val videoUrl: String,
    val bookmarked: Boolean
)
