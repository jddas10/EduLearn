package com.example.edulearn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edulearn.R
import com.example.edulearn.model.RecordedModel

class RecordedAdapter(private var list: List<RecordedModel>) :
    RecyclerView.Adapter<RecordedAdapter.RecordedViewHolder>() {

    inner class RecordedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val txtDuration: TextView = itemView.findViewById(R.id.txtDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recorded, parent, false)
        return RecordedViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordedViewHolder, position: Int) {
        val item = list[position]
        holder.txtTitle.text = item.title
        holder.txtDate.text = item.date
        holder.txtDuration.text = item.duration
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<RecordedModel>) {
        list = newList
        notifyDataSetChanged()
    }
}
