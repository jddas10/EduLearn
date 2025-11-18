package com.example.edulearn

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LiveLectureActivity : AppCompatActivity() {

    private val youtubeUrl = "https://youtube.com/@hitensadaniedu?si=WW538_ssJRHRjL2A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        val joinLive = findViewById<Button>(R.id.btnJoinLive)
        joinLive.setOnClickListener {
            openYouTubeChannel(youtubeUrl)
        }
    }

    private fun openYouTubeChannel(url: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            `package` = "com.google.android.youtube"
        }

        try {
            startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }
}
