package com.example.edulearn

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class AddQuizActivity : AppCompatActivity() {

    private val viewModel: AddQuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_quiz)

        val sessionManager = SessionManager(this)
        val titleField = findViewById<EditText>(R.id.etQuizTitle)
        val totalMarksField = findViewById<EditText>(R.id.etTotalMarks)
        val questionField = findViewById<EditText>(R.id.etQuestionText)
        val optAField = findViewById<EditText>(R.id.etOptA)
        val optBField = findViewById<EditText>(R.id.etOptB)
        val optCField = findViewById<EditText>(R.id.etOptC)
        val optDField = findViewById<EditText>(R.id.etOptD)
        val correctOptField = findViewById<EditText>(R.id.etCorrectOpt)
        val marksField = findViewById<EditText>(R.id.etQuestionMarks)
        val questionCount = findViewById<TextView>(R.id.tvQuestionCount)

        findViewById<Button>(R.id.btnAddQuestion).setOnClickListener {
            val questionText = questionField.text.toString().trim()
            val optA = optAField.text.toString().trim()
            val optB = optBField.text.toString().trim()
            val optC = optCField.text.toString().trim()
            val optD = optDField.text.toString().trim()
            val correctOpt = correctOptField.text.toString().trim().uppercase()
            val marks = marksField.text.toString().trim().toIntOrNull() ?: 0

            if (questionText.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                Toast.makeText(this, "Please fill all question fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addQuestion(
                QuizQuestionPayload(
                    questionText = questionText,
                    optA = optA,
                    optB = optB,
                    optC = optC,
                    optD = optD,
                    correctOpt = correctOpt,
                    marks = marks
                )
            )

            questionField.text?.clear()
            optAField.text?.clear()
            optBField.text?.clear()
            optCField.text?.clear()
            optDField.text?.clear()
            correctOptField.text?.clear()
            marksField.text?.clear()
        }

        findViewById<Button>(R.id.btnSubmitQuiz).setOnClickListener {
            val title = titleField.text.toString().trim()
            val totalMarks = totalMarksField.text.toString().trim().toIntOrNull() ?: 0
            val teacherId = intent.getIntExtra("teacher_id", 0).takeIf { it != 0 }
                ?: sessionManager.getUsername()?.toIntOrNull()
                ?: 1

            if (title.isEmpty() || totalMarks == 0) {
                Toast.makeText(this, "Please enter quiz title and total marks.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitQuiz(teacherId = teacherId, title = title, totalMarks = totalMarks)
        }

        viewModel.questions.observe(this) { questions ->
            questionCount.text = "Questions added: ${questions.size}"
        }

        viewModel.submissionStatus.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }
}
