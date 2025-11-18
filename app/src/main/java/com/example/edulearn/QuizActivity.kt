package com.example.edulearn

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.edulearn.model.QuizQuestion
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

class QuizActivity : AppCompatActivity() {

    private val questions = listOf(
        QuizQuestion("Android is developed by?", "Google", listOf("Microsoft", "Apple", "Oracle")),
        QuizQuestion("Which language is used for Android?", "Kotlin", listOf("Swift", "Ruby", "JavaScript")),
        QuizQuestion("XML is used for?", "UI Design", listOf("Database", "Networking", "Encryption")),
        QuizQuestion("Android Studio is developed by?", "JetBrains", listOf("Google LLC", "Microsoft", "Samsung"))
    )

    private lateinit var tvQuestion: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var btns: List<MaterialButton>
    private lateinit var btnNext: MaterialButton
    private lateinit var quizResult: TextView

    private var index = 0
    private var score = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        tvQuestion = findViewById(R.id.tvQuestion)
        tvProgress = findViewById(R.id.tvProgress)
        tvScore = findViewById(R.id.tvScore)
        progressBar = findViewById(R.id.progressQuiz)
        btns = listOf(
            findViewById(R.id.btnOption1),
            findViewById(R.id.btnOption2),
            findViewById(R.id.btnOption3),
            findViewById(R.id.btnOption4)
        )
        btnNext = findViewById(R.id.btnNext)
        quizResult = findViewById(R.id.quizResult)

        btns.forEach { b ->
            b.setOnClickListener {
                handleAnswer(b)
            }
        }

        btnNext.setOnClickListener {
            index++
            loadQuestion()
        }

        loadQuestion()
    }

    private fun loadQuestion() {
        if (index >= questions.size) {
            showResult()
            return
        }
        resetOptionButtons()
        val q = questions[index]
        val options = mutableListOf<String>()
        options.add(q.correct)
        options.addAll(q.wrong)
        options.shuffle()
        tvQuestion.text = "Q${index + 1}. ${q.question}"
        tvProgress.text = "Q${index + 1} / ${questions.size}"
        tvScore.text = "Score: $score"
        progressBar.progress = (((index).toFloat() / questions.size) * 100).toInt()
        for (i in 0 until 4) {
            btns[i].text = options[i]
            btns[i].isEnabled = true
            btns[i].alpha = 1f
            btns[i].setBackgroundColor(Color.parseColor("#111827"))
            btns[i].setTextColor(Color.parseColor("#E6EEF6"))
        }
        btnNext.visibility = android.view.View.GONE
    }

    private fun handleAnswer(button: MaterialButton) {
        disableOptions()
        val selected = button.text.toString()
        val correct = questions[index].correct
        if (selected == correct) {
            score++
            button.setBackgroundColor(Color.parseColor("#1E7F3A"))
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(Color.parseColor("#A82F2F"))
            button.setTextColor(Color.WHITE)
            val correctBtn = btns.find { it.text.toString() == correct }
            correctBtn?.setBackgroundColor(Color.parseColor("#1E7F3A"))
            correctBtn?.setTextColor(Color.WHITE)
        }
        tvScore.text = "Score: $score"
        btnNext.visibility = android.view.View.VISIBLE
        handler.postDelayed({
            index++
            loadQuestion()
        }, 1000)
    }

    private fun disableOptions() {
        btns.forEach {
            it.isEnabled = false
            it.alpha = 0.9f
        }
    }

    private fun resetOptionButtons() {
        btns.forEach {
            it.isEnabled = true
            it.alpha = 1f
            it.setBackgroundColor(Color.parseColor("#111827"))
            it.setTextColor(Color.parseColor("#E6EEF6"))
        }
    }

    private fun showResult() {
        tvQuestion.text = ""
        btns.forEach { it.visibility = android.view.View.GONE }
        btnNext.visibility = android.view.View.GONE
        progressBar.visibility = android.view.View.GONE
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.quizCard).visibility = android.view.View.GONE
        quizResult.visibility = android.view.View.VISIBLE
        quizResult.text = "🎉 Quiz Completed!\nYour Score: $score / ${questions.size}"
    }
}
