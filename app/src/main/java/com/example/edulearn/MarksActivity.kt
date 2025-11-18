package com.example.edulearn

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity



class MarksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marks)

        val tvMarks = findViewById<TextView>(R.id.tvMarks)
        val progress = findViewById<ProgressBar>(R.id.marksProgress)

        val marks = mapOf("Maths" to 85, "Science" to 90, "English" to 80)
        val average = marks.values.average().toInt()

        tvMarks.text = marks.entries.joinToString("\n") { "${it.key}: ${it.value}%" } +
                "\n\nAverage: $average%"
        progress.progress = average
    }
}
