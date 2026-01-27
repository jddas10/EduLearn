package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.view.animation.AnimationUtils
import android.widget.ImageView


class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var database: AppDatabase
    private lateinit var sessionManager: SessionManager

    // 👇 Role coming from RoleSelectionActivity
    private var loginRole: String = "STUDENT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)

        // 🔐 Already logged in → go to main
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // 🔹 Get role from intent
        loginRole = intent.getStringExtra("LOGIN_ROLE") ?: "STUDENT"

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)


        btnLogin.setOnClickListener {
            handleLogin()

        }






    }

    private fun handleLogin() {
        val username = etUsername.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                when (loginRole) {

                    // 👨‍🎓 STUDENT LOGIN
                    "STUDENT" -> {
                        val user = database.userDao().login(username, password)
                        if (user != null) {
                            sessionManager.saveSession(user.username, user.fullName)
                            Toast.makeText(this@LoginActivity, "Student login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Invalid student credentials", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // 👨‍🏫 TEACHER LOGIN (future-proof)
                    "TEACHER" -> {
                        // 🔒 For now using same DB
                        // 🔜 Later replace this with Teachers_Master DB check
                        val user = database.userDao().login(username, password)
                        if (user != null) {
                            sessionManager.saveSession(user.username, user.fullName)
                            Toast.makeText(this@LoginActivity, "Teacher login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "Invalid teacher credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
