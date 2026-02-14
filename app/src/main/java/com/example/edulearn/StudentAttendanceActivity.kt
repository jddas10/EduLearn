package com.example.edulearn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var previewView: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    private val isScanning = AtomicBoolean(false)
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val cam = perms[Manifest.permission.CAMERA] == true
            val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (cam && (fine || coarse)) {
                setupScanner()
            } else {
                showPermissionDeniedDialog()
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

        setupPinchToZoom()

        btnScan.setOnClickListener {
            if (isScanning.compareAndSet(false, true)) {
                runOnUiThread {
                    tvStatus.text = "🔍 Scanning for QR code..."
                    btnScan.isEnabled = false
                }
                checkPermissionsAndStart()
            }
        }
    }

    private fun setupPinchToZoom() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                camera?.let { cam ->
                    val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    val delta = detector.scaleFactor
                    val newZoom = currentZoom * delta

                    val minZoom = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                    val maxZoom = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 10f

                    val clampedZoom = max(minZoom, min(newZoom, maxZoom))
                    cam.cameraControl.setZoomRatio(clampedZoom)
                }
                return true
            }
        })

        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
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

    @OptIn(ExperimentalGetImage::class)
    private fun setupScanner() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && isScanning.get()) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val data = barcode.rawValue
                                        if (!data.isNullOrBlank() && isScanning.compareAndSet(true, false)) {
                                            runOnUiThread { processQrData(data) }
                                            break
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    if (isScanning.compareAndSet(true, false)) {
                                        runOnUiThread {
                                            Toast.makeText(this, "❌ QR scan failed", Toast.LENGTH_SHORT).show()
                                            resetUI()
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    } catch (e: Exception) {
                        imageProxy.close()
                        if (isScanning.compareAndSet(true, false)) {
                            runOnUiThread {
                                Toast.makeText(this, "⚠️ Scanner error", Toast.LENGTH_SHORT).show()
                                resetUI()
                            }
                        }
                    }
                }

                camera = cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                camera?.cameraControl?.setZoomRatio(1f)

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "❌ Camera setup failed", Toast.LENGTH_SHORT).show()
                    resetUI()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processQrData(data: String) {
        val parts = data.trim().split("|")
        if (parts.size >= 4 &&
            parts[0].equals("EDULEARN", ignoreCase = true) &&
            parts[1].isNotBlank() &&
            parts[2].isNotBlank()) {

            val sessionId = parts[1].trim()
            val nonce = parts[2].trim()

            tvStatus.text = "📍 Verifying location..."
            ensureGpsAndMark(sessionId, nonce)
        } else {
            Toast.makeText(this, "❌ Invalid QR code format", Toast.LENGTH_SHORT).show()
            resetUI()
        }
    }

    private fun ensureGpsAndMark(sessionId: String, nonce: String) {
        ensureLocationEnabled(
            onEnabled = {
                fetchLocation { lat, lng, acc ->
                    if (acc > 50) {
                        runOnUiThread {
                            Toast.makeText(this, "⚠️ GPS accuracy poor (${acc}m). Move to open area.", Toast.LENGTH_LONG).show()
                        }
                    }
                    val deviceId = sessionManager.getOrCreateDeviceId()
                    sendAttendanceRequest(sessionId, nonce, lat, lng, acc, deviceId)
                }
            },
            onFail = {
                Toast.makeText(this, "📍 Please enable GPS", Toast.LENGTH_SHORT).show()
                resetUI()
            }
        )
    }

    private fun ensureLocationEnabled(onEnabled: () -> Unit, onFail: () -> Unit) {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (enabled) {
            onEnabled()
            return
        }

        val req = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
            )
            .setAlwaysShow(true)
            .build()

        LocationServices.getSettingsClient(this)
            .checkLocationSettings(req)
            .addOnSuccessListener { onEnabled() }
            .addOnFailureListener {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                onFail()
            }
    }

    private fun fetchLocation(onResult: (Double, Double, Int) -> Unit) {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "📍 Location permission required", Toast.LENGTH_SHORT).show()
            resetUI()
            return
        }

        try {
            val tokenSource = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        onResult(loc.latitude, loc.longitude, loc.accuracy.toInt())
                    } else {
                        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                            .setMaxUpdates(1)
                            .build()

                        fused.requestLocationUpdates(
                            req,
                            object : com.google.android.gms.location.LocationCallback() {
                                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                                    fused.removeLocationUpdates(this)
                                    val l = result.lastLocation
                                    if (l != null) {
                                        onResult(l.latitude, l.longitude, l.accuracy.toInt())
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this@StudentAttendanceActivity, "📍 Location not available", Toast.LENGTH_SHORT).show()
                                            resetUI()
                                        }
                                    }
                                }
                            },
                            mainLooper
                        )
                    }
                }
                .addOnFailureListener {
                    runOnUiThread {
                        Toast.makeText(this, "❌ Location error", Toast.LENGTH_SHORT).show()
                        resetUI()
                    }
                }
        } catch (se: SecurityException) {
            Toast.makeText(this, "🚫 Location permission denied", Toast.LENGTH_SHORT).show()
            resetUI()
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun sendAttendanceRequest(
        sessionId: String,
        nonce: String,
        lat: Double,
        lng: Double,
        acc: Int,
        deviceId: String
    ) {
        tvStatus.text = "✅ Submitting attendance..."

        RetrofitClient.instance.attendanceMark(
            AttendanceMarkRequest(sessionId, nonce, lat, lng, acc, deviceId)
        ).enqueue(object : Callback<AttendanceMarkResponse> {

            override fun onResponse(call: Call<AttendanceMarkResponse>, response: Response<AttendanceMarkResponse>) {
                val body = response.body()

                if (response.code() == 401 || response.code() == 403) {
                    forceReLogin("🔒 Session expired. Login again.")
                    return
                }

                if (response.isSuccessful && body?.success == true) {
                    val distanceMsg = body.distanceM?.let { " (${it}m away)" } ?: ""
                    Toast.makeText(
                        this@StudentAttendanceActivity,
                        "✅ ${body.message ?: "Attendance marked"}$distanceMsg",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    val msg = body?.message ?: "❌ Failed to mark attendance"
                    if (msg.contains("session", true) || msg.contains("expired", true)) {
                        forceReLogin(msg)
                    } else {
                        tvStatus.text = msg
                        Toast.makeText(this@StudentAttendanceActivity, msg, Toast.LENGTH_LONG).show()
                        resetUI()
                    }
                }
            }

            override fun onFailure(call: Call<AttendanceMarkResponse>, t: Throwable) {
                tvStatus.text = "🌐 Network error. Check connection."
                Toast.makeText(this@StudentAttendanceActivity, "🌐 Network error: ${t.message}", Toast.LENGTH_LONG).show()
                resetUI()
            }
        })
    }

    private fun forceReLogin(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        sessionManager.logout()
        val i = Intent(this, RoleSelectionActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔒 Permissions Required")
            .setMessage("Camera and Location permissions are required to scan QR codes and verify your location for attendance.\n\nPlease enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                resetUI()
            }
            .setNegativeButton("Cancel") { _, _ ->
                resetUI()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetUI() {
        isScanning.set(false)
        runOnUiThread {
            btnScan.isEnabled = true
            btnScan.text = "📷 Scan QR Code"
            tvStatus.text = "Ready to scan"
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
        camera = null
    }

    override fun onResume() {
        super.onResume()
        if (isScanning.get() && hasLocationPermission()) {
            setupScanner()
        }
    }

    override fun onDestroy() {
        isScanning.set(false)
        cameraProvider?.unbindAll()
        camera = null
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}