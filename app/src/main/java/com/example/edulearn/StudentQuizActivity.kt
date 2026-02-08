package com.example.edulearn

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class StudentQuizActivity : AppCompatActivity() {

    private val viewModel: StudentQuizViewModel by viewModels()
    private var quizId: Int = 0
    private var studentId: Int = 0

    private lateinit var tvQuestion: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvScore: TextView
    private lateinit var rgOptions: RadioGroup
    private lateinit var rbA: RadioButton
    private lateinit var rbB: RadioButton
    private lateinit var rbC: RadioButton
    private lateinit var rbD: RadioButton
    private lateinit var btnNext: Button
    private lateinit var tvCompleted: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_quiz)

        quizId = intent.getIntExtra("quiz_id", 0)
        studentId = intent.getIntExtra("student_id", 0)

        tvQuestion = findViewById(R.id.tvStudentQuestion)
        tvProgress = findViewById(R.id.tvStudentProgress)
        tvScore = findViewById(R.id.tvStudentScore)
        rgOptions = findViewById(R.id.rgOptions)
        rbA = findViewById(R.id.rbOptionA)
        rbB = findViewById(R.id.rbOptionB)
        rbC = findViewById(R.id.rbOptionC)
        rbD = findViewById(R.id.rbOptionD)
        btnNext = findViewById(R.id.btnNextQuestion)
        tvCompleted = findViewById(R.id.tvQuizCompleted)

        rgOptions.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                val selectedText = findViewById<RadioButton>(checkedId)?.text?.toString() ?: ""
                viewModel.selectAnswer(selectedText)
            }
        }

        btnNext.setOnClickListener {
            if (rgOptions.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.moveToNext()
                updateQuestionUi()
            }
        }

        viewModel.questions.observe(this) { updateQuestionUi() }
        viewModel.currentScore.observe(this) { score ->
            tvScore.text = "Score: $score"
        }

        if (quizId != 0) {
            viewModel.loadQuiz(quizId)
        }
    }

    private fun updateQuestionUi() {
        val question = viewModel.getCurrentQuestion()
        val questions = viewModel.questions.value.orEmpty()
        val index = viewModel.currentIndex.value ?: 0

        if (question == null || index >= questions.size) {
            tvQuestion.visibility = View.GONE
            rgOptions.visibility = View.GONE
            btnNext.visibility = View.GONE
            tvProgress.visibility = View.GONE
            tvCompleted.visibility = View.VISIBLE
            tvCompleted.text = "Quiz completed! Final Score: ${viewModel.currentScore.value}"
            viewModel.syncProgress(studentId, quizId, status = "Completed")
            return
        }

        tvQuestion.text = question.questionText
        tvProgress.text = "Question ${index + 1} / ${questions.size}"
        rbA.text = "A. ${question.optA}"
        rbB.text = "B. ${question.optB}"
        rbC.text = "C. ${question.optC}"
        rbD.text = "D. ${question.optD}"
        rgOptions.clearCheck()
        tvCompleted.visibility = View.GONE
    }
}