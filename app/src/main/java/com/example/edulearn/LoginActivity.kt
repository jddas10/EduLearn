package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var sessionManager: SessionManager
    private var loginRole: String = "STUDENT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        // ✅ only once
        loginRole = intent.getStringExtra("LOGIN_ROLE")?.uppercase() ?: "STUDENT"

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener { handleLogin() }
    }

    private fun handleLogin() {
        val username = etUsername.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            return
        }

        val loginRequest = LoginRequest(username, password, loginRole)

        RetrofitClient.instance.loginUser(loginRequest).enqueue(object : Callback<AuthResponse> {

            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    val msg = errorBody?.let { parseErrorMessage(it) }
                        ?: "Login failed (${response.code()})"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    return
                }

                val auth = response.body()

                // ✅ success can be via success=true OR token/user present (node backend)
                val isSuccess =
                    (auth?.success == true) ||
                            (!auth?.token.isNullOrBlank()) ||
                            (auth?.user != null)

                if (!isSuccess) {
                    Toast.makeText(
                        this@LoginActivity,
                        auth?.message ?: "Invalid credentials",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val displayName = auth?.name?.takeIf { it.isNotBlank() } ?: username
                sessionManager.saveSession(username, displayName)

                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(
                    this@LoginActivity,
                    "Connection Error: ${t.localizedMessage ?: "Unknown"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun parseErrorMessage(body: String): String? {
        return try {
            JSONObject(body).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
