package com.amdg.jajanyuks.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val NEARBY_RADIUS_METERS = 1_000.0

data class NearbyVendor(val vendor: Vendor, val distanceMeters: Double?)

// Vendors within 1 km of the buyer, closest first.
// If the buyer's location is unknown, show every vendor (without a distance).
// ponytail: filters on the phone, fine for a few hundred live vendors; switch to a geohash query beyond that.
fun nearbyVendors(vendors: List<Vendor>, myLat: Double?, myLng: Double?): List<NearbyVendor> {
    if (myLat == null || myLng == null) return vendors.map { NearbyVendor(it, null) }
    return vendors
        .map { NearbyVendor(it, distanceMeters(myLat, myLng, it.lat, it.lng)) }
        .filter { it.distanceMeters!! <= NEARBY_RADIUS_METERS }
        .sortedBy { it.distanceMeters }
}

// Straight-line distance on the Earth's surface (haversine formula).
fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * 6_371_000.0 * asin(sqrt(a))
}
