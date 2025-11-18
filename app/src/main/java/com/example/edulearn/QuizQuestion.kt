package com.example.edulearn.model

data class QuizQuestion(
    val question: String,
    val correct: String,
    val wrong: List<String>
)
