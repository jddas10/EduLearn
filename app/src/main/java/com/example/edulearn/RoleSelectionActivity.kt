package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.edulearn.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            val next = if (sessionManager.getRole() == "TEACHER")
                TeacherMainActivity::class.java
            else
                MainActivity::class.java

            startActivity(Intent(this, next).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }


        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStudent.setOnClickListener { openLoginScreen("STUDENT") }
        binding.btnTeacher.setOnClickListener { openLoginScreen("TEACHER") }
    }

    private fun openLoginScreen(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("LOGIN_ROLE", role.uppercase())
        startActivity(intent)

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
}
