package com.example.edulearn

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false)
    val username: String,
    val name: String,
    val role: String
)