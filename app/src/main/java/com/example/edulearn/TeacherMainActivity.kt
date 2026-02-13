package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class TeacherMainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.init(this)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn() || (sessionManager.getRole() ?: "").uppercase() != "TEACHER") {
            startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setContentView(R.layout.activity_teacher_main)

        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)
        val nameToShow = sessionManager.getFullName() ?: sessionManager.getUsername() ?: "Teacher"
        welcomeTitle.text = "Welcome Back, $nameToShow!"

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val settingsIcon = findViewById<ImageView>(R.id.settingsIcon)

        val btnAttendance = findViewById<Button>(R.id.btnAttendance)
        val btnRecorded = findViewById<Button>(R.id.btnRecorded)
        val btnLive = findViewById<Button>(R.id.btnLive)
        val btnQuiz = findViewById<Button>(R.id.btnQuiz)
        val btnMarks = findViewById<Button>(R.id.btnMarks)
        val btnHomework = findViewById<Button>(R.id.btnHomework)

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> performLogout()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        settingsIcon.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
            val dialog = AlertDialog.Builder(this, R.style.SettingsDialogTheme)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            val btnChangeTheme = dialogView.findViewById<Button>(R.id.btnChangeTheme)
            btnChangeTheme?.setOnClickListener { dialog.dismiss() }
        }

        btnRecorded.setOnClickListener { safeStart(RecordedLectureActivity::class.java) }
        btnLive.setOnClickListener { safeStart(LiveLectureActivity::class.java) }
        btnAttendance.setOnClickListener { safeStart(TeacherAttendanceActivity::class.java) }
        btnMarks.setOnClickListener { safeStart(MarksActivity::class.java) }
        btnHomework.setOnClickListener { safeStart(HomeworkActivity::class.java) }

        btnQuiz.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Quiz Options")
                .setMessage("Create a new quiz?")
                .setPositiveButton("Create Quiz") { _, _ ->
                    val intent = Intent(this, AddQuizActivity::class.java)
                    val teacherId = sessionManager.getUserId()
                    intent.putExtra("teacher_id", if (teacherId == 0) 1 else teacherId)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun safeStart(target: Class<*>) {
        try {
            startActivity(Intent(this, target))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        sessionManager.logout()
        startActivity(Intent(this, RoleSelectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}
