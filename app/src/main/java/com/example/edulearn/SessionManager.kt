package com.example.edulearn

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "EduLearnSession"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"

        // ✅ NEW
        private const val KEY_ROLE = "role"
    }

    fun saveSession(username: String, fullName: String?, role: String): Boolean {
        return prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_ROLE, role)
            .commit()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getFullName(): String? {
        return prefs.getString(KEY_FULL_NAME, null)
    }

    fun getRole(): String {
        return prefs.getString(KEY_ROLE, "") ?: ""
    }

    fun logout(): Boolean {
        return prefs.edit().clear().commit()
    }
}
