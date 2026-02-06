package com.example.edulearn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val ANIMATION_DURATION = 900L
    private val STAGGER_DELAY = 150L

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
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

        setContentView(R.layout.activity_main)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
        btnQuiz.setOnClickListener { safeStart(QuizActivity::class.java) }
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
        googleSignInClient.signOut().addOnCompleteListener(this) {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                onGoogleSignInSuccess(account)
            } catch (e: ApiException) {
                val code = e.statusCode
                val msg = GoogleSignInStatusCodes.getStatusCodeString(code)
                Toast.makeText(this, "Google sign-in failed: $msg ($code)", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Google sign-in error: $msg ($code)", e)
            }
        }
    }

    private fun onGoogleSignInSuccess(account: GoogleSignInAccount?) {
        val name = account?.displayName ?: "User"
        val prefs = getSharedPreferences("edulearn_prefs", MODE_PRIVATE)
        prefs.edit().putString("user_name", name).apply()
        updateWelcomeName()
        Toast.makeText(this, "Logged in as $name", Toast.LENGTH_SHORT).show()
    }

    private fun updateWelcomeName() {
        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)
        val fullName = sessionManager.getFullName()
        val username = sessionManager.getUsername()
        val nameToShow = fullName ?: username ?: "User"
        welcomeTitle.text = "Welcome Back, $nameToShow!"
    }
}
