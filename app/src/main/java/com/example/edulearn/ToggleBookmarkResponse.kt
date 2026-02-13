package com.example.edulearn

data class ToggleBookmarkResponse(
    val success: Boolean,
    val bookmarked: Boolean,
    val message: String? = null
)
