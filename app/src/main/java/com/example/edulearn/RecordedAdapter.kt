package com.example.edulearn.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edulearn.LectureModel
import com.example.edulearn.R

class RecordedAdapter(
    private var list: List<LectureModel>,
    private val isTeacher: Boolean,
    private val onPlay: (LectureModel) -> Unit,
    private val onToggleBookmark: (LectureModel) -> Unit,
    private val onDelete: (LectureModel) -> Unit
) : RecyclerView.Adapter<RecordedAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val txtTitle: TextView = v.findViewById(R.id.txtTitle)
        val txtSubject: TextView = v.findViewById(R.id.txtSubject)
        val txtDate: TextView = v.findViewById(R.id.txtDate)
        val imgPlay: ImageView = v.findViewById(R.id.imgIcon)
        val imgBookmark: ImageView = v.findViewById(R.id.imgBookmark)
        val imgDelete: ImageView? = v.findViewById(R.id.imgDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recorded, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]

        holder.txtTitle.text = item.title
        holder.txtSubject.text = if (item.subject.isNotBlank()) item.subject else item.category
        holder.txtDate.text = item.createdAt

        holder.imgBookmark.alpha = if (item.bookmarked) 1f else 0.35f

        holder.imgPlay.setOnClickListener { onPlay(item) }
        holder.itemView.setOnClickListener { onPlay(item) }

        holder.imgBookmark.setOnClickListener {
            if (!isTeacher) onToggleBookmark(item)
        }

        holder.imgDelete?.visibility = if (isTeacher) View.VISIBLE else View.GONE
        holder.imgDelete?.setOnClickListener {
            if (isTeacher) onDelete(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<LectureModel>) {
        list = newList
        notifyDataSetChanged()
    }

    fun setBookmarked(lectureId: Int, bookmarked: Boolean) {
        val idx = list.indexOfFirst { it.id == lectureId }
        if (idx != -1) {
            val mutable = list.toMutableList()
            val old = mutable[idx]
            mutable[idx] = old.copy(bookmarked = bookmarked)
            list = mutable
            notifyItemChanged(idx)
        }
    }
}
