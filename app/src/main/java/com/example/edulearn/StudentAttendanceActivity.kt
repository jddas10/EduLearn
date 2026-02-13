package com.example.edulearn

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@registerForActivityResult
        val parts = text.split("|")
        if (parts.size < 4 || parts[0] != "EDULEARN") {
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        val sessionId = parts[1]
        val nonce = parts[2]
        markAttendance(sessionId, nonce)
    }

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val camOk = perms[Manifest.permission.CAMERA] == true
        val locOk = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (camOk && locOk) startScan()
        else Toast.makeText(this, "Camera + Location permissions required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_student_attendance)

        tvStatus = findViewById(R.id.tvStatus)
        btnScan = findViewById(R.id.btnScanQr)

        btnScan.setOnClickListener { ensurePermissionsAndScan() }
    }

    private fun ensurePermissionsAndScan() {
        val camGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (camGranted && (fineGranted || coarseGranted)) {
            startScan()
            return
        }

        requestPerms.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setPrompt("Scan Attendance QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setBarcodeImageEnabled(false)
        scanLauncher.launch(options)
    }

    private fun markAttendance(sessionId: String, nonce: String) {
        if (sessionManager.getToken().isNullOrBlank()) {
            Toast.makeText(this, "Login again", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Getting location..."

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            tvStatus.text = "Location permission missing"
            return
        }

        locationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) {
                tvStatus.text = "Location not available"
                return@addOnSuccessListener
            }

            val req = AttendanceMarkRequest(
                sessionId = sessionId,
                nonce = nonce,
                lat = loc.latitude,
                lng = loc.longitude,
                accuracyM = loc.accuracy.toInt(),
                deviceId = sessionManager.getOrCreateDeviceId()
            )

            tvStatus.text = "Marking attendance..."

            RetrofitClient.instance.attendanceMark(req)
                .enqueue(object : Callback<AttendanceMarkResponse> {
                    override fun onResponse(call: Call<AttendanceMarkResponse>, response: Response<AttendanceMarkResponse>) {
                        val body = response.body()
                        if (response.isSuccessful && body?.success == true) {
                            tvStatus.text = body.message ?: "Attendance marked"
                        } else {
                            tvStatus.text = body?.message ?: "Failed"
                        }
                    }

                    override fun onFailure(call: Call<AttendanceMarkResponse>, t: Throwable) {
                        tvStatus.text = "Connection error"
                    }
                })
        }.addOnFailureListener {
            tvStatus.text = "Location error"
        }
    }
}
