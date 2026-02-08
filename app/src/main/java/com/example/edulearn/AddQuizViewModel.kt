package com.example.edulearn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddQuizViewModel : ViewModel() {
    private val _questions = MutableLiveData<List<QuizQuestionPayload>>(emptyList())
    val questions: LiveData<List<QuizQuestionPayload>> = _questions

    private val _submissionStatus = MutableLiveData<String?>()
    val submissionStatus: LiveData<String?> = _submissionStatus

    fun addQuestion(question: QuizQuestionPayload) {
        val updated = _questions.value.orEmpty().toMutableList()
        updated.add(question)
        _questions.value = updated
    }

    fun removeQuestion(position: Int) {
        val updated = _questions.value.orEmpty().toMutableList()
        if (position in updated.indices) {
            updated.removeAt(position)
            _questions.value = updated
        }
    }

    fun submitQuiz(teacherId: Int, title: String, totalMarks: Int, quizId: Int? = null) {
        val request = QuizCreateRequest(
            quizId = quizId,
            teacherId = teacherId,
            title = title,
            totalMarks = totalMarks,
            questions = _questions.value.orEmpty()
        )

        RetrofitClient.instance.createQuiz(request).enqueue(object : Callback<QuizCreateResponse> {
            override fun onResponse(
                call: Call<QuizCreateResponse>,
                response: Response<QuizCreateResponse>
            ) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    _submissionStatus.value = "Quiz saved (ID: ${body.quizId})"
                } else {
                    _submissionStatus.value = body?.message ?: "Failed to save quiz"
                }
            }

            override fun onFailure(call: Call<QuizCreateResponse>, t: Throwable) {
                _submissionStatus.value = "Network error: ${t.localizedMessage}"
            }
        })
    }
}
