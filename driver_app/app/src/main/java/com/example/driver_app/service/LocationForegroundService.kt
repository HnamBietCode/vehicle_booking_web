package com.example.driver_app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.PowerManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.driver_app.R
import com.example.driver_app.data.model.DriverLocationRequest
import com.example.driver_app.data.repo.DriverJobsRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

class LocationForegroundService : Service() {

    private val channelId = "driver_location"
    private lateinit var fused: FusedLocationProviderClient
    private var callback: LocationCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripType = intent?.getStringExtra("tripType") ?: return START_NOT_STICKY
        val tripId = intent.getLongExtra("tripId", -1)
        if (tripId <= 0) return START_NOT_STICKY

        startForeground(1, buildNotification())

        acquireWakeLock()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(10_000L)
            .setMaxUpdateDelayMillis(10_000L)
            .setWaitForAccurateLocation(false)
            .build()
        callback?.let { fused.removeLocationUpdates(it) }
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                sendLocation(tripType, tripId, loc)
            }
        }

        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    sendLocation(tripType, tripId, loc)
                }
            }

        fused.requestLocationUpdates(request, callback!!, mainLooper)
        return START_REDELIVER_INTENT
    }

    private fun sendLocation(tripType: String, tripId: Long, loc: Location) {
        scope.launch {
            DriverJobsRepository.sendLocation(
                DriverLocationRequest(
                    tripType = tripType,
                    tripId = tripId,
                    lat = loc.latitude,
                    lng = loc.longitude,
                    speed = loc.speed.toDouble(),
                    bearing = loc.bearing.toDouble(),
                    accuracy = loc.accuracy.toDouble(),
                    recordedAt = Instant.now().toString()
                )
            )
        }
    }

    override fun onDestroy() {
        callback?.let { fused.removeLocationUpdates(it) }
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            channelId,
            "Driver Location",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dang di chuyen")
            .setContentText("Ung dung dang gui vi tri tai xe")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "driver_app:location")
            wakeLock?.setReferenceCounted(false)
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }
}
