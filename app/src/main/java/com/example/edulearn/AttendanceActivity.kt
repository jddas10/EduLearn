package com.example.edulearn

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AttendanceActivity : AppCompatActivity() {

    private lateinit var capturedImageView: ImageView
    private lateinit var imagePlaceholderText: TextView
    private lateinit var btnCapturePhoto: Button
    private lateinit var btnRetakePhoto: Button
    private lateinit var btnMarkAttendance: Button
    private lateinit var attendanceMarkedText: TextView

    // ✅ Naya camera launcher – startActivityForResult ka replacement
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.extras?.get("data") as? Bitmap

                if (imageBitmap != null) {
                    capturedImageView.setImageBitmap(imageBitmap)
                    imagePlaceholderText.visibility = View.GONE
                    btnRetakePhoto.visibility = View.VISIBLE
                    btnMarkAttendance.visibility = View.VISIBLE
                    btnCapturePhoto.visibility = View.GONE
                } else {
                    Toast.makeText(this, "can't get image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Camera canceled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        capturedImageView = findViewById(R.id.capturedImageView)
        imagePlaceholderText = findViewById(R.id.imagePlaceholderText)
        btnCapturePhoto = findViewById(R.id.btnCapturePhoto)
        btnRetakePhoto = findViewById(R.id.btnRetakePhoto)
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance)
        attendanceMarkedText = findViewById(R.id.attendanceMarkedText)

        btnCapturePhoto.setOnClickListener { dispatchTakePictureIntent() }
        btnRetakePhoto.setOnClickListener { dispatchTakePictureIntent() }
        btnMarkAttendance.setOnClickListener { showSuccess() }
    }

    private fun dispatchTakePictureIntent() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                // ✅ Naya launcher use karo
                cameraLauncher.launch(takePictureIntent)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Camera open karne me error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccess() {
        btnRetakePhoto.visibility = View.GONE
        btnMarkAttendance.visibility = View.GONE
        imagePlaceholderText.visibility = View.GONE
        capturedImageView.visibility = View.GONE
        attendanceMarkedText.visibility = View.VISIBLE

        // ✅ Ensure layout me ye id wala view REAL me hai
        val successGif: pl.droidsonroids.gif.GifImageView = findViewById(R.id.successGif)
        successGif.apply {
            visibility = View.VISIBLE
            setImageResource(R.raw.yee)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val drawable = successGif.drawable
        if (drawable is pl.droidsonroids.gif.GifDrawable) {
            drawable.loopCount = 0
        }

        Toast.makeText(this, "Attendance Marked Successfully ✅", Toast.LENGTH_LONG).show()
    }
}
