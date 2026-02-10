package com.example.edulearn

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentQuizActivity : AppCompatActivity() {

    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var rgOptions: RadioGroup
    private lateinit var rbA: RadioButton
    private lateinit var rbB: RadioButton
    private lateinit var rbC: RadioButton
    private lateinit var rbD: RadioButton
    private lateinit var btnNext: Button
    private lateinit var tvCompleted: TextView

    // Result Overlay
    private lateinit var resultOverlay: View
    private lateinit var btnResultDone: View
    private lateinit var tvPercentScore: TextView
    private lateinit var tvResultDetail: TextView

    private var quizId: Int = 0
    private var index = 0
    private var score = 0

    private data class Q(
        val questionText: String,
        val optA: String,
        val optB: String,
        val optC: String,
        val optD: String,
        val correctOpt: String,
        val marks: Int
    )

    private var questions: List<Q> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_quiz)

        progressBar = findViewById(R.id.progressStudentQuiz)
        tvProgress = findViewById(R.id.tvStudentProgress)
        tvScore = findViewById(R.id.tvStudentScore)
        tvQuestion = findViewById(R.id.tvStudentQuestion)

        rgOptions = findViewById(R.id.rgOptions)
        rbA = findViewById(R.id.rbOptionA)
        rbB = findViewById(R.id.rbOptionB)
        rbC = findViewById(R.id.rbOptionC)
        rbD = findViewById(R.id.rbOptionD)

        btnNext = findViewById(R.id.btnNextQuestion)
        tvCompleted = findViewById(R.id.tvQuizCompleted)

        resultOverlay = findViewById(R.id.resultOverlay)
        btnResultDone = findViewById(R.id.btnResultDone)
        tvPercentScore = findViewById(R.id.tvPercentScore)
        tvResultDetail = findViewById(R.id.tvResultDetail)

        resultOverlay.visibility = View.GONE

        quizId = intent.getIntExtra("quiz_id", 0)
        if (quizId == 0) {
            Toast.makeText(this, "Quiz ID missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        btnNext.setOnClickListener { onNext() }

        // Only one option: Done
        btnResultDone.setOnClickListener { finish() }

        fetchQuiz(quizId)
    }

    private fun fetchQuiz(quizId: Int) {
        RetrofitClient.instance.getQuiz(quizId).enqueue(object : Callback<QuizDetailResponse> {
            override fun onResponse(call: Call<QuizDetailResponse>, response: Response<QuizDetailResponse>) {
                Log.d("STUDENT_QUIZ", "HTTP ${response.code()}")

                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("STUDENT_QUIZ", "ErrorBody: $err")
                    Toast.makeText(this@StudentQuizActivity, "Failed to load quiz (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                val body = response.body()
                if (body?.success != true) {
                    Toast.makeText(this@StudentQuizActivity, "Quiz not found", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                questions = body.questions.map {
                    Q(
                        questionText = it.questionText,
                        optA = it.optA,
                        optB = it.optB,
                        optC = it.optC,
                        optD = it.optD,
                        correctOpt = it.correctOpt,
                        marks = it.marks
                    )
                }

                if (questions.isEmpty()) {
                    Toast.makeText(this@StudentQuizActivity, "No questions in this quiz", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }

                index = 0
                score = 0
                showQuestion()
            }

            override fun onFailure(call: Call<QuizDetailResponse>, t: Throwable) {
                Log.e("STUDENT_QUIZ", "Failure: ${t.message}", t)
                Toast.makeText(this@StudentQuizActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    private fun showQuestion() {
        // ensure overlay hidden
        resultOverlay.visibility = View.GONE

        // show main UI
        progressBar.visibility = View.VISIBLE
        tvProgress.visibility = View.VISIBLE
        tvScore.visibility = View.VISIBLE
        tvQuestion.visibility = View.VISIBLE
        rgOptions.visibility = View.VISIBLE
        btnNext.visibility = View.VISIBLE
        tvCompleted.visibility = View.GONE

        val q = questions[index]
        tvQuestion.text = q.questionText

        rbA.text = q.optA
        rbB.text = q.optB
        rbC.text = q.optC
        rbD.text = q.optD

        rgOptions.clearCheck()

        tvProgress.text = "Question ${index + 1} of ${questions.size}"
        tvScore.text = "Score: $score"

        val percent = (((index).toFloat() / questions.size) * 100f).toInt()
        progressBar.progress = percent
    }

    private fun onNext() {
        if (index >= questions.size) return

        val checkedId = rgOptions.checkedRadioButtonId
        if (checkedId == -1) {
            Toast.makeText(this, "Select one option", Toast.LENGTH_SHORT).show()
            return
        }

        val q = questions[index]
        val selectedOpt = when (checkedId) {
            R.id.rbOptionA -> "A"
            R.id.rbOptionB -> "B"
            R.id.rbOptionC -> "C"
            R.id.rbOptionD -> "D"
            else -> ""
        }

        if (selectedOpt.equals(q.correctOpt, ignoreCase = true)) {
            score += (if (q.marks > 0) q.marks else 1)
        }

        index++

        if (index >= questions.size) {
            showCompleted()
        } else {
            showQuestion()
        }
    }

    private fun showCompleted() {
        // hide all background "bakwas"
        progressBar.visibility = View.GONE
        tvProgress.visibility = View.GONE
        tvScore.visibility = View.GONE
        tvQuestion.visibility = View.GONE
        rgOptions.visibility = View.GONE
        btnNext.visibility = View.GONE
        tvCompleted.visibility = View.GONE

        showResultOverlay()
    }

    private fun showResultOverlay() {
        val total = questions.size


        val percent = if (total == 0) 0 else ((score * 100) / total).coerceAtMost(100)

        tvPercentScore.text = "$percent% Score"
        tvResultDetail.text = "You attempted $total questions and $score answer is correct."

        resultOverlay.visibility = View.VISIBLE
    }
}
