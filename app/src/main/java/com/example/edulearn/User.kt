package com.example.edulearn

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false)
    val username: String,
    val password: String,
    val email: String? = null,
    val fullName: String? = null
)