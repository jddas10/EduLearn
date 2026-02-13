package com.example.edulearn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine || coarse) {
                ensureGpsThenFetch()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(this)
        sessionManager = SessionManager(this)
        setContentView(R.layout.activity_student_attendance)
        ensureLocationFlow()
    }

    private fun ensureLocationFlow() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            ensureGpsThenFetch()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun ensureGpsThenFetch() {
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Turn on Location (GPS)", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        fetchFreshLocation { lat, lng, acc ->
            val sessionId = intent.getStringExtra("sessionId") ?: ""
            val nonce = intent.getStringExtra("nonce") ?: ""
            if (sessionId.isBlank() || nonce.isBlank()) {
                Toast.makeText(this, "Invalid session", Toast.LENGTH_SHORT).show()
                return@fetchFreshLocation
            }

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            markAttendance(sessionId, nonce, lat, lng, acc, deviceId)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun fetchFreshLocation(onResult: (Double, Double, Int) -> Unit) {
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
                            val l = result.lastLocation
                            if (l == null) {
                                Toast.makeText(this@StudentAttendanceActivity, "Location not available, try again", Toast.LENGTH_SHORT).show()
                            } else {
                                onResult(l.latitude, l.longitude, l.accuracy.toInt())
                            }
                        }
                    }, mainLooper)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Location error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markAttendance(sessionId: String, nonce: String, lat: Double, lng: Double, acc: Int, deviceId: String) {
        RetrofitClient.instance.attendanceMark(
            AttendanceMarkRequest(
                sessionId = sessionId,
                nonce = nonce,
                lat = lat,
                lng = lng,
                accuracyM = acc,
                deviceId = deviceId
            )
        ).enqueue(object : Callback<AttendanceMarkResponse> {
            override fun onResponse(call: Call<AttendanceMarkResponse>, response: Response<AttendanceMarkResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    Toast.makeText(this@StudentAttendanceActivity, body.message ?: "Marked", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@StudentAttendanceActivity, body?.message ?: "Mark failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AttendanceMarkResponse>, t: Throwable) {
                Toast.makeText(this@StudentAttendanceActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        ensureLocationFlow()
    }
}
