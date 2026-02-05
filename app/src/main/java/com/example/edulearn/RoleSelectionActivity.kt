package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.edulearn.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Student button click
        binding.btnStudent.setOnClickListener {
            openLoginScreen("STUDENT")
        }

        // Teacher button click
        binding.btnTeacher.setOnClickListener {
            openLoginScreen("TEACHER")
        }
    }

    private fun openLoginScreen(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("LOGIN_ROLE", role.uppercase())
        startActivity(intent)

        // Smooth transition (optional but premium feel)
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }
}
