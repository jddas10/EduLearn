package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val ANIMATION_DURATION = 900L
    private val STAGGER_DELAY = 150L

    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, RoleSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        if (sessionManager.getRole() == "TEACHER") {
            startActivity(Intent(this, TeacherMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val titleText = findViewById<TextView>(R.id.titleText)
        val welcomeCard = findViewById<MaterialCardView>(R.id.welcomeCard)
        val buttonGrid = findViewById<GridLayout>(R.id.buttonGrid)
        val recentHeading = findViewById<TextView>(R.id.recentHeading)
        val recentCard = findViewById<MaterialCardView>(R.id.recentCard)
        val settingsIcon = findViewById<ImageView>(R.id.settingsIcon)

        val btnAttendance = findViewById<Button>(R.id.btnAttendance)
        val btnLive = findViewById<Button>(R.id.btnLive)
        val btnRecorded = findViewById<Button>(R.id.btnRecorded)
        val btnQuiz = findViewById<Button>(R.id.btnQuiz)
        val btnMarks = findViewById<Button>(R.id.btnMarks)
        val btnHomework = findViewById<Button>(R.id.btnHomework)

        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> performLogout()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fun prepareForAnimation(view: View?) {
            view?.apply {
                alpha = 0f
                translationY = 50f
            }
        }

        prepareForAnimation(titleText)
        prepareForAnimation(welcomeCard)
        prepareForAnimation(buttonGrid)
        prepareForAnimation(recentHeading)
        prepareForAnimation(recentCard)

        fun animateIn(view: View?, delay: Long) {
            view?.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(ANIMATION_DURATION)
                ?.setStartDelay(delay)
                ?.start()
        }

        animateIn(titleText, 0)
        animateIn(welcomeCard, STAGGER_DELAY)
        animateIn(buttonGrid, STAGGER_DELAY * 2)
        animateIn(recentHeading, STAGGER_DELAY * 3)
        animateIn(recentCard, STAGGER_DELAY * 4)

        fun safeStart(target: Class<*>) {
            try {
                startActivity(Intent(this, target))
            } catch (e: Exception) {
                Log.e(TAG, "Error starting activity: ${e.message}")
            }
        }

        btnAttendance.setOnClickListener { safeStart(AttendanceActivity::class.java) }
        btnLive.setOnClickListener { safeStart(LiveLectureActivity::class.java) }
        btnRecorded.setOnClickListener { safeStart(RecordedLectureActivity::class.java) }

        btnQuiz.setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter Quiz ID (e.g. 1)"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

            MaterialAlertDialogBuilder(this)
                .setTitle("Start Quiz")
                .setMessage("Take ID from your teacher")
                .setView(input)
                .setPositiveButton("Start") { _, _ ->
                    val quizId = input.text.toString().trim().toIntOrNull() ?: 0
                    if (quizId == 0) {
                        Toast.makeText(this, "Invalid Quiz ID", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val i = Intent(this, StudentQuizActivity::class.java)
                    i.putExtra("quiz_id", quizId)
                    startActivity(i)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnMarks.setOnClickListener { safeStart(MarksActivity::class.java) }
        btnHomework.setOnClickListener { safeStart(HomeworkActivity::class.java) }

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

        updateWelcomeName()
    }

    private fun performLogout() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun updateWelcomeName() {
        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)
        val fullName = sessionManager.getFullName()
        val username = sessionManager.getUsername()
        val nameToShow = fullName ?: username ?: "User"
        welcomeTitle.text = "Welcome Back, $nameToShow!"
    }
}
