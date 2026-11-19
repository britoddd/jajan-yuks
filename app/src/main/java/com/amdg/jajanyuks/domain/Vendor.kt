package com.amdg.jajanyuks.domain

// A `data class` gets equals/hashCode/toString/copy for free — ideal for plain models.
data class Vendor(
    val id: String,
    val name: String?, // null if the seller has no name yet
    val lat: Double,
    val lng: Double,
    val isLive: Boolean = true,
    val updatedAt: Long? = null, // epoch millis of the last location update
    val statusText: String? = null, // seller's note for buyers, set by voice
)

// A live seller's phone writes at least every 4 min (LocationService heartbeat).
// Silent for 10+ min = the app was probably killed without going offline, so hide it.
const val STALE_AFTER_MILLIS = 10 * 60_000L

fun Vendor.isOnline(now: Long = System.currentTimeMillis()) =
    isLive && (updatedAt == null || now - updatedAt < STALE_AFTER_MILLIS) // null = write still pending
