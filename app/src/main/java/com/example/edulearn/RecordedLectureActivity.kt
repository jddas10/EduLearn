package com.example.edulearn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edulearn.adapter.RecordedAdapter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class RecordedLectureActivity : AppCompatActivity() {

    private lateinit var adapter: RecordedAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var sessionManager: SessionManager

    private val fullList = mutableListOf<LectureModel>()

    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) showMetaDialogAndUpload(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_recorded)

        val isTeacher = (sessionManager.getRole() ?: "").uppercase() == "TEACHER"

        recycler = findViewById(R.id.recyclerRecorded)
        recycler.layoutManager = GridLayoutManager(this, 2)

        adapter = RecordedAdapter(
            list = fullList,
            isTeacher = isTeacher,
            onPlay = { lecture ->
                val i = Intent(this, VideoPlayerActivity::class.java)
                i.putExtra("title", lecture.title)
                i.putExtra("url", lecture.videoUrl)
                startActivity(i)
            },
            onToggleBookmark = { lecture -> toggleBookmark(lecture.id) },
            onDelete = { lecture -> confirmDelete(lecture.id) }
        )

        recycler.adapter = adapter

        searchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })

        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val btnUpload = findViewById<View>(R.id.btnUpload)
        if (isTeacher) {
            btnUpload.visibility = View.VISIBLE
            btnUpload.setOnClickListener { pickVideo.launch("video/*") }
        } else {
            btnUpload.visibility = View.GONE
        }

        fetchLectures()
    }

    private fun fetchLectures() {
        RetrofitClient.instance.getLectures().enqueue(object : Callback<LecturesResponse> {
            override fun onResponse(call: Call<LecturesResponse>, response: Response<LecturesResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    fullList.clear()
                    fullList.addAll(body.lectures)
                    filterList(searchView.query?.toString().orEmpty())
                } else {
                    Toast.makeText(this@RecordedLectureActivity, "Failed to load lectures", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LecturesResponse>, t: Throwable) {
                Toast.makeText(this@RecordedLectureActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterList(q: String) {
        val query = q.trim().lowercase()
        if (query.isEmpty()) {
            adapter.updateList(fullList)
            return
        }
        val filtered = fullList.filter {
            it.title.lowercase().contains(query) ||
                    it.subject.lowercase().contains(query) ||
                    it.category.lowercase().contains(query)
        }
        adapter.updateList(filtered)
    }

    private fun toggleBookmark(lectureId: Int) {
        if ((sessionManager.getRole() ?: "").uppercase() != "STUDENT") {
            Toast.makeText(this, "Only students can bookmark", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.toggleBookmark(ToggleBookmarkRequest(lectureId))
            .enqueue(object : Callback<ToggleBookmarkResponse> {
                override fun onResponse(
                    call: Call<ToggleBookmarkResponse>,
                    response: Response<ToggleBookmarkResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {
                        val idx = fullList.indexOfFirst { it.id == lectureId }
                        if (idx != -1) {
                            fullList[idx] = fullList[idx].copy(bookmarked = body.bookmarked)
                        }
                        adapter.setBookmarked(lectureId, body.bookmarked)
                    } else {
                        Toast.makeText(this@RecordedLectureActivity, "Bookmark failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ToggleBookmarkResponse>, t: Throwable) {
                    Toast.makeText(this@RecordedLectureActivity, "Connection error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmDelete(lectureId: Int) {
        if ((sessionManager.getRole() ?: "").uppercase() != "TEACHER") return

        AlertDialog.Builder(this)
            .setTitle("Delete Lecture")
            .setMessage("Are you sure you want to delete this lecture?")
            .setPositiveButton("Delete") { _, _ -> deleteLecture(lectureId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLecture(lectureId: Int) {
        RetrofitClient.instance.deleteLecture(lectureId).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@RecordedLectureActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    fetchLectures()
                } else {
                    Toast.makeText(this@RecordedLectureActivity, body?.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                Toast.makeText(this@RecordedLectureActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showMetaDialogAndUpload(uri: Uri) {
        val titleEt = EditText(this)
        val subjectEt = EditText(this)
        val categoryEt = EditText(this)

        titleEt.hint = "Title"
        subjectEt.hint = "Subject"
        categoryEt.hint = "Category"

        val wrap = android.widget.LinearLayout(this)
        wrap.orientation = android.widget.LinearLayout.VERTICAL
        val p = (16 * resources.displayMetrics.density).toInt()
        wrap.setPadding(p, p, p, p)
        wrap.addView(titleEt)
        wrap.addView(subjectEt)
        wrap.addView(categoryEt)

        AlertDialog.Builder(this)
            .setTitle("Upload Lecture")
            .setView(wrap)
            .setPositiveButton("Upload") { _, _ ->
                val title = titleEt.text?.toString()?.trim().orEmpty()
                val subject = subjectEt.text?.toString()?.trim().orEmpty()
                val category = categoryEt.text?.toString()?.trim().orEmpty()

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                uploadLecture(uri, title, subject, category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadLecture(uri: Uri, title: String, subject: String, category: String) {
        val file = uriToFile(uri) ?: run {
            Toast.makeText(this, "Video read failed", Toast.LENGTH_SHORT).show()
            return
        }

        val videoPart = MultipartBody.Part.createFormData(
            "video",
            file.name,
            file.asRequestBody("video/*".toMediaTypeOrNull())
        )

        val t: RequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val s: RequestBody = subject.toRequestBody("text/plain".toMediaTypeOrNull())
        val c: RequestBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

        RetrofitClient.instance.uploadLecture(videoPart, t, s, c).enqueue(object : Callback<SimpleResponse> {
            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RecordedLectureActivity, "Uploaded", Toast.LENGTH_SHORT).show()
                    fetchLectures()
                } else {
                    Toast.makeText(this@RecordedLectureActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                Toast.makeText(this@RecordedLectureActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            val outFile = File(cacheDir, "upload_${System.currentTimeMillis()}.mp4")
            FileOutputStream(outFile).use { output ->
                input.use { it.copyTo(output) }
            }
            outFile
        } catch (_: Exception) {
            null
        }
    }
}