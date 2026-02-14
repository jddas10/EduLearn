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
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

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
    private val qrExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val exportExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var sessionId: String? = null
    private var nonceTtl: Int = 15
    private var isActive = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            val sid = sessionId ?: return
            if (!isActive) return
            requestNonce(sid)
            val delayMs = max(1000, (nonceTtl - 2) * 1000).toLong()
            handler.postDelayed(this, delayMs)
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

        updateUIState(false)
    }

    private fun updateUIState(active: Boolean) {
        isActive = active
        btnStart.isEnabled = !active
        btnClose.isEnabled = active
        btnExport.isEnabled = active

        if (!active) {
            btnStart.alpha = 1f
            btnClose.alpha = 0.5f
            btnExport.alpha = 0.5f
        } else {
            btnStart.alpha = 0.5f
            btnClose.alpha = 1f
            btnExport.alpha = 1f
        }
    }

    private fun startAttendance() {
        if (sessionManager.getToken().isNullOrBlank()) {
            Toast.makeText(this, "🔒 Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "📍 Getting location..."
        btnStart.isEnabled = false

        val token = CancellationTokenSource()
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    tvStatus.text = "❌ Location not available"
                    btnStart.isEnabled = true
                    return@addOnSuccessListener
                }

                val title = etTitle.text?.toString()?.trim().orEmpty().ifBlank { "Attendance Session" }

                val req = AttendanceStartRequest(
                    title = title,
                    lat = loc.latitude,
                    lng = loc.longitude,
                    radiusM = 50,
                    accuracyM = 30,
                    durationMinutes = 15,
                    nonceTtlSeconds = 15
                )

                tvStatus.text = "🚀 Starting session..."

                RetrofitClient.instance.attendanceStart(req)
                    .enqueue(object : Callback<AttendanceStartResponse> {
                        override fun onResponse(call: Call<AttendanceStartResponse>, response: Response<AttendanceStartResponse>) {
                            val body = response.body()
                            if (response.isSuccessful && body?.success == true && !body.sessionId.isNullOrBlank()) {
                                sessionId = body.sessionId
                                nonceTtl = body.nonceTtlSeconds ?: 15
                                showQr(body.qr.orEmpty())
                                tvStatus.text = "✅ Active: ${body.sessionId} | Range: 50m | QR: ${nonceTtl}s"
                                updateUIState(true)
                                handler.removeCallbacks(refreshRunnable)
                                val delayMs = max(1000, (nonceTtl - 2) * 1000).toLong()
                                handler.postDelayed(refreshRunnable, delayMs)
                            } else {
                                tvStatus.text = "❌ ${body?.message ?: "Failed to start"}"
                                btnStart.isEnabled = true
                                Toast.makeText(this@TeacherAttendanceActivity, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<AttendanceStartResponse>, t: Throwable) {
                            tvStatus.text = "🌐 Connection error"
                            btnStart.isEnabled = true
                            Toast.makeText(this@TeacherAttendanceActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .addOnFailureListener {
                tvStatus.text = "❌ Location error"
                btnStart.isEnabled = true
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestNonce(sid: String) {
        RetrofitClient.instance.attendanceNonce(AttendanceNonceRequest(sessionId = sid, ttlSeconds = nonceTtl))
            .enqueue(object : Callback<AttendanceNonceResponse> {
                override fun onResponse(call: Call<AttendanceNonceResponse>, response: Response<AttendanceNonceResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true && !body.qr.isNullOrBlank()) {
                        showQr(body.qr)
                        runOnUiThread {
                            tvStatus.text = "✅ Active: $sid | Range: 50m | QR refreshed"
                        }
                    } else if (response.code() == 404 || body?.success == false) {
                        handler.removeCallbacks(refreshRunnable)
                        runOnUiThread {
                            tvStatus.text = "⏹ Session expired or closed"
                            updateUIState(false)
                        }
                    }
                }
                override fun onFailure(call: Call<AttendanceNonceResponse>, t: Throwable) {
                    runOnUiThread {
                        tvStatus.text = "⚠️ QR refresh failed. Retrying..."
                    }
                }
            })
    }

    private fun closeAttendance() {
        val sid = sessionId ?: return
        btnClose.isEnabled = false
        tvStatus.text = "⏹ Closing session..."

        RetrofitClient.instance.attendanceClose(AttendanceCloseRequest(sessionId = sid))
            .enqueue(object : Callback<AttendanceCloseResponse> {
                override fun onResponse(call: Call<AttendanceCloseResponse>, response: Response<AttendanceCloseResponse>) {
                    handler.removeCallbacks(refreshRunnable)
                    val body = response.body()
                    tvStatus.text = if (response.isSuccessful && body?.success == true) {
                        "✅ ${body.message ?: "Session closed"}"
                    } else {
                        "⚠️ ${body?.message ?: "Close failed"}"
                    }
                    updateUIState(false)
                    ivQr.setImageBitmap(null)
                }

                override fun onFailure(call: Call<AttendanceCloseResponse>, t: Throwable) {
                    tvStatus.text = "🌐 Connection error"
                    Toast.makeText(this@TeacherAttendanceActivity, "Failed to close session", Toast.LENGTH_SHORT).show()
                    btnClose.isEnabled = true
                }
            })
    }

    private fun exportCsv() {
        val sid = sessionId ?: run {
            Toast.makeText(this, "⚠️ Start attendance first", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "⬇ Exporting CSV..."
        btnExport.isEnabled = false

        exportExecutor.execute {
            try {
                val url = "${RetrofitClient.baseUrl()}attendance/export?sessionId=$sid"
                val token = sessionManager.getToken().orEmpty()

                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val bytes = response.body?.bytes() ?: ByteArray(0)
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(null), "attendance_${sid}_$ts.csv")

                FileOutputStream(file).use { it.write(bytes) }

                val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

                runOnUiThread {
                    tvStatus.text = "✅ CSV exported successfully"
                    btnExport.isEnabled = true

                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, "Export Attendance CSV"))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvStatus.text = "❌ Export failed"
                    btnExport.isEnabled = true
                    Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showQr(text: String) {
        if (text.isBlank()) return

        qrExecutor.execute {
            try {
                val bitmap = generateQrBitmapOptimized(text, 900, 900)
                runOnUiThread {
                    ivQr.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateQrBitmapOptimized(text: String, width: Int, height: Int): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val pixels = IntArray(width * height)
        for (i in pixels.indices) {
            val x = i % width
            val y = i / width
            pixels[i] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    override fun onPause() {
        super.onPause()
        if (isActive) {
            handler.removeCallbacks(refreshRunnable)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isActive && sessionId != null) {
            val delayMs = max(1000, (nonceTtl - 2) * 1000).toLong()
            handler.postDelayed(refreshRunnable, delayMs)
        }
    }

    override fun onDestroy() {
        isActive = false
        handler.removeCallbacks(refreshRunnable)
        qrExecutor.shutdown()
        exportExecutor.shutdown()
        super.onDestroy()
    }
}