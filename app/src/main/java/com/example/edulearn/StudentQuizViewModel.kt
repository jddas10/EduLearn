package com.example.edulearn

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentQuizViewModel : ViewModel() {

    val questions = MutableLiveData<List<QuizQuestionDto>>(emptyList())
    val currentIndex = MutableLiveData(0)
    val currentScore = MutableLiveData(0)

    private var selectedAnswer: String? = null

    fun loadQuiz(quizId: Int) {
        RetrofitClient.instance.getQuiz(quizId).enqueue(object : Callback<QuizDetailResponse> {
            override fun onResponse(call: Call<QuizDetailResponse>, response: Response<QuizDetailResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    questions.value = response.body()?.questions.orEmpty()
                }
            }

            override fun onFailure(call: Call<QuizDetailResponse>, t: Throwable) {}
        })
    }

    fun selectAnswer(answer: String) {
        selectedAnswer = answer
    }

    fun moveToNext() {
        val currentList = questions.value.orEmpty()
        val index = currentIndex.value ?: 0
        if (index !in currentList.indices) return

        val correct = currentList[index].correctOpt.trim().uppercase()
        val selectedOpt = selectedAnswer?.trim()?.firstOrNull()?.toString()?.uppercase()

        if (selectedOpt == correct) {
            currentScore.value = (currentScore.value ?: 0) + currentList[index].marks
        }

        currentIndex.value = index + 1
        selectedAnswer = null
    }

    fun getCurrentQuestion(): QuizQuestionDto? {
        val list = questions.value.orEmpty()
        val idx = currentIndex.value ?: 0
        return if (idx in list.indices) list[idx] else null
    }

    fun syncProgress(sId: Int, qId: Int, status: String) {
        val payload = QuizProgressRequest(sId, qId, currentScore.value ?: 0, status)
        RetrofitClient.instance.syncQuizProgress(payload).enqueue(object : Callback<QuizProgressResponse> {
            override fun onResponse(call: Call<QuizProgressResponse>, response: Response<QuizProgressResponse>) {}
            override fun onFailure(call: Call<QuizProgressResponse>, t: Throwable) {}
        })
    }
}
