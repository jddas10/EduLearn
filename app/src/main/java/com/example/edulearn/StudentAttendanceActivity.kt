package com.example.edulearn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var previewView: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button
    private lateinit var cameraExecutor: ExecutorService

    private var isScanning = false
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val cam = perms[Manifest.permission.CAMERA] == true
            val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (cam && (fine || coarse)) {
                setupScanner()
            } else {
                Toast.makeText(this, "Camera & Location permissions required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_student_attendance)

        previewView = findViewById(R.id.previewView)
        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScanQr)
        cameraExecutor = Executors.newSingleThreadExecutor()

        btnScan.setOnClickListener {
            if (!isScanning) {
                isScanning = true
                tvStatus.text = "Scanning..."
                btnScan.isEnabled = false
                checkPermissionsAndStart()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (cam && (fine || coarse)) {
            setupScanner()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setupScanner() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null && isScanning) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                barcode.rawValue?.let { data ->
                                    isScanning = false
                                    runOnUiThread { processQrData(data) }
                                }
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Scanner setup failed", Toast.LENGTH_SHORT).show()
                    resetUI()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processQrData(data: String) {
        val parts = data.split("|")
        if (parts.size >= 2) {
            val sessionId = parts[0]
            val nonce = parts[1]
            tvStatus.text = "Locating device..."
            ensureGpsAndMark(sessionId, nonce)
        } else {
            Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show()
            resetUI()
        }
    }

    private fun ensureGpsAndMark(sessionId: String, nonce: String) {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            resetUI()
            return
        }

        fetchLocation { lat, lng, acc ->
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            sendAttendanceRequest(sessionId, nonce, lat, lng, acc, deviceId)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun fetchLocation(onResult: (Double, Double, Int) -> Unit) {
        val tokenSource = CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult(loc.latitude, loc.longitude, loc.accuracy.toInt())
                } else {
                    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                        .setMaxUpdates(1)
                        .build()
                    fused.requestLocationUpdates(req, object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            fused.removeLocationUpdates(this)
                            result.lastLocation?.let { onResult(it.latitude, it.longitude, it.accuracy.toInt()) }
                        }
                    }, mainLooper)
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(this, "Location error", Toast.LENGTH_SHORT).show()
                    resetUI()
                }
            }
    }

    private fun sendAttendanceRequest(sessionId: String, nonce: String, lat: Double, lng: Double, acc: Int, deviceId: String) {
        tvStatus.text = "Submitting..."
        RetrofitClient.instance.attendanceMark(
            AttendanceMarkRequest(sessionId, nonce, lat, lng, acc, deviceId)
        ).enqueue(object : Callback<AttendanceMarkResponse> {
            override fun onResponse(call: Call<AttendanceMarkResponse>, response: Response<AttendanceMarkResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    Toast.makeText(this@StudentAttendanceActivity, body.message ?: "Success", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    tvStatus.text = body?.message ?: "Failed"
                    resetUI()
                }
            }

            override fun onFailure(call: Call<AttendanceMarkResponse>, t: Throwable) {
                tvStatus.text = "Network Error"
                resetUI()
            }
        })
    }

    private fun resetUI() {
        isScanning = false
        btnScan.isEnabled = true
        btnScan.text = "📷 Rescan QR"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}