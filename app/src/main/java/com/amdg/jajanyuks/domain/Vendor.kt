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
