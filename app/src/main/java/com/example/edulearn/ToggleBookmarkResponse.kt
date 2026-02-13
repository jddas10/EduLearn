package com.example.edulearn.model

data class ToggleBookmarkResponse(
    val success: Boolean,
    val bookmarked: Boolean,
    val message: String? = null
)
