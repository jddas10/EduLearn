package com.example.edulearn

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeacherAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvStatus: TextView
    private lateinit var ivQr: ImageView
    private lateinit var etTitle: EditText
    private lateinit var btnStart: Button
    private lateinit var btnClose: Button
    private lateinit var btnExport: Button

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val handler = Handler(Looper.getMainLooper())

    private var sessionId: String? = null
    private var nonceTtl: Int = 20

    private val refreshRunnable = object : Runnable {
        override fun run() {
            val sid = sessionId ?: return
            requestNonce(sid)
            handler.postDelayed(this, (nonceTtl * 1000).toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_teacher_attendance)

        tvStatus = findViewById(R.id.tvTeacherStatus)
        ivQr = findViewById(R.id.ivQr)
        etTitle = findViewById(R.id.etAttendanceTitle)
        btnStart = findViewById(R.id.btnStartAttendance)
        btnClose = findViewById(R.id.btnCloseAttendance)
        btnExport = findViewById(R.id.btnExportCsv)

        btnStart.setOnClickListener { startAttendance() }
        btnClose.setOnClickListener { closeAttendance() }
        btnExport.setOnClickListener { exportCsv() }

        btnClose.isEnabled = false
        btnExport.isEnabled = false
    }

    private fun startAttendance() {
        if (sessionManager.getToken().isNullOrBlank()) {
            Toast.makeText(this, "Login again", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Getting location..."

        locationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                tvStatus.text = "Location not available"
                return@addOnSuccessListener
            }

            val title = etTitle.text?.toString()?.trim().orEmpty().ifBlank { "Attendance" }

            val req = AttendanceStartRequest(
                title = title,
                lat = loc.latitude,
                lng = loc.longitude,
                radiusM = 100,
                accuracyM = 50,
                durationMinutes = 10,
                nonceTtlSeconds = 20
            )

            tvStatus.text = "Starting..."

            RetrofitClient.instance.attendanceStart(req)
                .enqueue(object : Callback<AttendanceStartResponse> {
                    override fun onResponse(call: Call<AttendanceStartResponse>, response: Response<AttendanceStartResponse>) {
                        val body = response.body()
                        if (response.isSuccessful && body?.success == true && !body.sessionId.isNullOrBlank()) {
                            sessionId = body.sessionId
                            nonceTtl = body.nonceTtlSeconds ?: 20
                            showQr(body.qr.orEmpty())
                            tvStatus.text = "Active: ${body.sessionId}"
                            btnClose.isEnabled = true
                            btnExport.isEnabled = true
                            handler.removeCallbacks(refreshRunnable)
                            handler.postDelayed(refreshRunnable, (nonceTtl * 1000).toLong())
                        } else {
                            tvStatus.text = body?.message ?: "Failed"
                        }
                    }

                    override fun onFailure(call: Call<AttendanceStartResponse>, t: Throwable) {
                        tvStatus.text = "Connection error"
                    }
                })
        }.addOnFailureListener {
            tvStatus.text = "Location error"
        }
    }

    private fun requestNonce(sid: String) {
        RetrofitClient.instance.attendanceNonce(AttendanceNonceRequest(sessionId = sid, ttlSeconds = nonceTtl))
            .enqueue(object : Callback<AttendanceNonceResponse> {
                override fun onResponse(call: Call<AttendanceNonceResponse>, response: Response<AttendanceNonceResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true && !body.qr.isNullOrBlank()) {
                        showQr(body.qr)
                    }
                }
                override fun onFailure(call: Call<AttendanceNonceResponse>, t: Throwable) {}
            })
    }

    private fun closeAttendance() {
        val sid = sessionId ?: return
        RetrofitClient.instance.attendanceClose(AttendanceCloseRequest(sessionId = sid))
            .enqueue(object : Callback<AttendanceCloseResponse> {
                override fun onResponse(call: Call<AttendanceCloseResponse>, response: Response<AttendanceCloseResponse>) {
                    handler.removeCallbacks(refreshRunnable)
                    tvStatus.text = "Closed"
                    btnClose.isEnabled = false
                }
                override fun onFailure(call: Call<AttendanceCloseResponse>, t: Throwable) {
                    Toast.makeText(this@TeacherAttendanceActivity, "Connection error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun exportCsv() {
        val sid = sessionId ?: run {
            Toast.makeText(this, "Start attendance first", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "${RetrofitClient.baseUrl()}attendance/export?sessionId=$sid"

        Thread {
            try {
                val client = OkHttpClient()
                val token = sessionManager.getToken().orEmpty()
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")

                val bytes = resp.body?.bytes() ?: ByteArray(0)

                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(null), "attendance_${sid}_$ts.csv")
                FileOutputStream(file).use { it.write(bytes) }

                val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

                runOnUiThread {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, "Export Attendance CSV"))
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun showQr(text: String) {
        if (text.isBlank()) return
        ivQr.setImageBitmap(generateQrBitmap(text, 900, 900))
    }

    private fun generateQrBitmap(text: String, width: Int, height: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        super.onDestroy()
    }
}
