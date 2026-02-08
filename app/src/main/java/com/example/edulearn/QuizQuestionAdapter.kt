package com.example.edulearn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class QuizQuestionAdapter(
    private var items: List<QuizQuestionPayload>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<QuizQuestionAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardQuestion)
        val questionTitle: TextView = view.findViewById(R.id.tvQuestionTitle)
        val questionMeta: TextView = view.findViewById(R.id.tvQuestionMeta)
        val optionA: TextView = view.findViewById(R.id.tvOptionA)
        val optionB: TextView = view.findViewById(R.id.tvOptionB)
        val optionC: TextView = view.findViewById(R.id.tvOptionC)
        val optionD: TextView = view.findViewById(R.id.tvOptionD)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val item = items[position]
        holder.questionTitle.text = item.questionText
        holder.questionMeta.text = "Correct: ${item.correctOpt} • Marks: ${item.marks}"
        holder.optionA.text = "A. ${item.optA}"
        holder.optionB.text = "B. ${item.optB}"
        holder.optionC.text = "C. ${item.optC}"
        holder.optionD.text = "D. ${item.optD}"
        holder.card.setOnLongClickListener {
            onRemove(position)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<QuizQuestionPayload>) {
        items = newItems
        notifyDataSetChanged()
    }
}
