package com.amdg.jajanyuks.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// The buyer's own location as a Flow, only while someone collects it (i.e. the map is on screen).
// Callers must have location permission first.
@SuppressLint("MissingPermission")
fun Context.locationUpdates(intervalMillis: Long): Flow<Location> = callbackFlow {
    val client = LocationServices.getFusedLocationProviderClient(this@locationUpdates)
    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { trySend(it) }
        }
    }
    // High accuracy (GPS): only runs while the map is on screen, and walking directions need it.
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    awaitClose { client.removeLocationUpdates(callback) }
}
