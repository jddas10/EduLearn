package com.example.edulearn

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.getOrNull

class StudentQuizViewModel : ViewModel() {
    val questions = MutableLiveData<List<QuizQuestion>>()
    val currentIndex = MutableLiveData(0)
    val currentScore = MutableLiveData(0)
    private var selectedAnswer: String? = null

    fun loadQuiz(quizId: Int) {
        RetrofitClient.instance.getQuestions(quizId).enqueue(object : Callback<QuizResponse> {
            override fun onResponse(call: Call<QuizResponse>, response: Response<QuizResponse>) {
                if (response.isSuccessful) {
                    questions.value = response.body()?.questions
                }
            }
            override fun onFailure(call: Call<QuizResponse>, t: Throwable) {}
        })
    }

    fun selectAnswer(answer: String) {
        selectedAnswer = answer
    }

    fun moveToNext() {
        val currentList = questions.value ?: return
        val index = currentIndex.value ?: 0
        if (index < currentList.size) {
            val correct = currentList[index].correctOpt.trim()
            if (selectedAnswer?.trim()?.startsWith(correct, ignoreCase = true) == true) {
                currentScore.value = (currentScore.value ?: 0) + currentList[index].marks
            }
            currentIndex.value = index + 1
            selectedAnswer = null
        }
    }

    fun getCurrentQuestion() = questions.value?.getOrNull(currentIndex.value ?: 0)

    fun syncProgress(sId: Int, qId: Int, stat: String) {
        val payload = QuizSubmissionPayload(sId, qId, currentScore.value ?: 0, stat)
        RetrofitClient.instance.syncQuizProgress(payload).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
}