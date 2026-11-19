package com.amdg.jajanyuks.location

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

// Foreground service: keeps sending the seller's location while they are live,
// even when the screen is off. Android requires a visible notification for it.
class LocationService : Service() {

    private val container get() = (application as JajanYuksApp).container
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var uid: String? = null // the seller who went live; kept so we can mark them offline later

    private var last: Location? = null
    // MainScope: coroutines on the main thread, cancelled together in onDestroy.
    private val scope = MainScope()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            last = result.lastLocation ?: return
            send()
        }
    }

    private fun send() {
        val uid = uid ?: return
        val location = last ?: return
        container.vendorRepository.updateLocation(uid, location.latitude, location.longitude)
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        uid = container.auth.currentUser?.uid
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.live_channel_name))
                .build(),
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.live_notification_title))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)

        // Every ~15 s at most, and only after moving ~20 m (saves battery and data).
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000)
            .setMinUpdateIntervalMillis(10_000)
            .setMinUpdateDistanceMeters(20f)
            .build()
        try {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf() // permission was revoked
            return START_NOT_STICKY
        }
        // A seller standing still gets no updates (20 m rule), so re-send the last spot every 4 min.
        // Buyers hide sellers silent for 10+ min (Vendor.isOnline).
        scope.launch {
            while (true) {
                delay(4 * 60_000L)
                send()
            }
        }
        running.value = true
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        scope.cancel()
        // If the process is killed instead, this never runs; buyers then hide the stale vendor after 10 min.
        uid?.let { container.vendorRepository.goOffline(it) }
        running.value = false
        super.onDestroy()
    }

    // `companion object` = members shared by the class, like Java's static.
    companion object {
        private const val CHANNEL_ID = "live_status"
        private const val NOTIFICATION_ID = 1
        private val running = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = running
    }
}
