package com.example.edulearn

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edulearn.adapter.RecordedAdapter
import com.example.edulearn.model.RecordedModel

class RecordedLectureActivity : AppCompatActivity() {

    private lateinit var adapter: RecordedAdapter
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorded)

        // RecyclerView setup
        recycler = findViewById(R.id.recyclerRecorded)
        recycler.layoutManager = GridLayoutManager(this, 2) // 2-column grid like screenshot

        // Sample data (replace with real source later)
        val data = listOf(
            RecordedModel("Algebra Basics", "Mathematics I", "5 Nov", "45 mins"),
            RecordedModel("Algebra - Lecture 1", "Lecture I", "3 Nov", "40 mins"),
            RecordedModel("Algebra Basics - Lecture 2", "Mathematics I", "30 Oct", "45 mins"),
            RecordedModel("History of Rome - Part 3", "History", "25 Oct", "45 mins"),
            RecordedModel("History of Rome", "Culture", "22 Oct", "45 mins"),
            RecordedModel("Algebra Basics", "Mathematics I", "18 Oct", "45 mins"),
            RecordedModel("Histora Dome - Part2", "Lecture", "12 Oct", "65 mins"),
            RecordedModel("Algebraces - Lecture 1", "Mathematics I", "5 Oct", "45 mins")
        )

        adapter = RecordedAdapter(data)
        recycler.adapter = adapter

        // Back button (exists in layout)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
