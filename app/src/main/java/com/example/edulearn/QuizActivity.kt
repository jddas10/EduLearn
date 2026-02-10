package com.example.edulearn

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QuizActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btns: List<MaterialButton>
    private lateinit var btnNext: MaterialButton
    private lateinit var quizResult: TextView
    private lateinit var quizCard: MaterialCardView

    private val handler = Handler(Looper.getMainLooper())

    private var index = 0
    private var score = 0

    private data class Q(
        val question: String,
        val correct: String,
        val wrong: List<String>
    )

    private var questions: List<Q> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        btns.forEach { b -> b.setOnClickListener { handleAnswer(b) } }
        btnNext.setOnClickListener {
            index++
            loadQuestion()
        }

        val quizId = intent.getIntExtra("quiz_id", 0)
        if (quizId == 0) {
            Toast.makeText(this, "Quiz ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fetchQuiz(quizId)
    }

    private fun fetchQuiz(quizId: Int) {
        RetrofitClient.instance.getQuiz(quizId).enqueue(object : Callback<QuizDetailResponse> {
            override fun onResponse(call: Call<QuizDetailResponse>, response: Response<QuizDetailResponse>) {
                Log.d("QUIZ_FETCH", "HTTP ${response.code()}")

                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("QUIZ_FETCH", "ErrorBody: $err")
                    Toast.makeText(this@QuizActivity, "Failed to load quiz: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                val body = response.body()
                if (body?.success != true) {
                    Toast.makeText(this@QuizActivity, "Quiz not found", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                questions = body.questions.map { dto ->
                    val correct = when (dto.correctOpt.uppercase()) {
                        "A" -> dto.optA
                        "B" -> dto.optB
                        "C" -> dto.optC
                        "D" -> dto.optD
                        else -> dto.optA
                    }
                    val wrong = listOf(dto.optA, dto.optB, dto.optC, dto.optD).filter { it != correct }
                    Q(dto.questionText, correct, wrong)
                }

                if (questions.isEmpty()) {
                    Toast.makeText(this@QuizActivity, "No questions in this quiz", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                index = 0
                score = 0
                loadQuestion()
            }

            override fun onFailure(call: Call<QuizDetailResponse>, t: Throwable) {
                Log.e("QUIZ_FETCH", "Failure: ${t.message}", t)
                Toast.makeText(this@QuizActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                finish()
            }
        })
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
            btns[i].visibility = View.VISIBLE
        }

        btnNext.visibility = View.GONE
        quizResult.visibility = View.GONE
        quizCard.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
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
        btnNext.visibility = View.VISIBLE

        handler.postDelayed({
            index++
            loadQuestion()
        }, 800)
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
        btns.forEach { it.visibility = View.GONE }
        btnNext.visibility = View.GONE
        progressBar.visibility = View.GONE
        quizCard.visibility = View.GONE
        quizResult.visibility = View.VISIBLE
        quizResult.text = "🎉 Quiz Completed!\nYour Score: $score / ${questions.size}"
    }
}
