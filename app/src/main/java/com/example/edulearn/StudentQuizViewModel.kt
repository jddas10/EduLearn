package com.example.edulearn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentQuizViewModel : ViewModel() {
    private val _questions = MutableLiveData<List<QuizQuestionDto>>(emptyList())
    val questions: LiveData<List<QuizQuestionDto>> = _questions

    private val _currentIndex = MutableLiveData(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _currentScore = MutableLiveData(0)
    val currentScore: LiveData<Int> = _currentScore

    private val selectedAnswers = mutableMapOf<Int, String>()

    fun loadQuiz(quizId: Int) {
        RetrofitClient.instance.getQuiz(quizId).enqueue(object : Callback<QuizDetailResponse> {
            override fun onResponse(
                call: Call<QuizDetailResponse>,
                response: Response<QuizDetailResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    _questions.value = response.body()?.questions.orEmpty()
                    _currentIndex.value = 0
                }
            }

            override fun onFailure(call: Call<QuizDetailResponse>, t: Throwable) {
                // No-op: UI will handle errors if needed.
            }
        })
    }

    fun selectAnswer(answer: String) {
        val question = getCurrentQuestion() ?: return
        val previous = selectedAnswers[question.id]
        if (previous == answer) return

        var score = _currentScore.value ?: 0
        if (previous != null && previous.equals(question.correctOpt, ignoreCase = true)) {
            score -= question.marks
        }
        if (answer.equals(question.correctOpt, ignoreCase = true)) {
            score += question.marks
        }

        selectedAnswers[question.id] = answer
        _currentScore.value = score
    }

    fun moveToNext() {
        val nextIndex = (_currentIndex.value ?: 0) + 1
        _currentIndex.value = nextIndex
    }

    fun getCurrentQuestion(): QuizQuestionDto? {
        val index = _currentIndex.value ?: 0
        return _questions.value?.getOrNull(index)
    }

    fun syncProgress(studentId: Int, quizId: Int, status: String = "Started") {
        val score = _currentScore.value ?: 0
        val request = QuizProgressRequest(
            studentId = studentId,
            quizId = quizId,
            currentScore = score,
            status = status
        )

        RetrofitClient.instance.syncQuizProgress(request).enqueue(object : Callback<QuizProgressResponse> {
            override fun onResponse(
                call: Call<QuizProgressResponse>,
                response: Response<QuizProgressResponse>
            ) {
                // Fire-and-forget.
            }

            override fun onFailure(call: Call<QuizProgressResponse>, t: Throwable) {
                // Fire-and-forget.
            }
        })
    }
}
