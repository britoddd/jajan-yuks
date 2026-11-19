package com.amdg.jajanyuks.data

import android.util.Log
import com.amdg.jajanyuks.domain.Vendor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// The vendor document id is the seller's auth uid, so Security Rules can check ownership easily.
class VendorRepository(firestore: FirebaseFirestore) {
    private val vendors = firestore.collection("vendors")

    // Fire-and-forget: Firestore queues the write and sends it when online.
    fun updateLocation(uid: String, lat: Double, lng: Double) {
        write(
            uid,
            mapOf(
                "ownerId" to uid,
                "isLive" to true,
                "location" to GeoPoint(lat, lng),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
    }

    fun goOffline(uid: String) {
        write(uid, mapOf("ownerId" to uid, "isLive" to false, "updatedAt" to FieldValue.serverTimestamp()))
    }

    // `callbackFlow` turns a callback-based listener into a Flow.
    // The listener is removed automatically when nobody collects anymore (awaitClose).
    // ponytail: returns every live vendor; add the 1 km geohash query in sprint 2.
    fun liveVendors(): Flow<List<Vendor>> = callbackFlow {
        val registration = vendors.whereEqualTo("isLive", true).addSnapshotListener { snapshot, error ->
            if (snapshot == null) {
                Log.w(TAG, "Live vendors listener failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot.documents.mapNotNull { it.toVendor() })
        }
        awaitClose { registration.remove() }
    }

    private fun write(uid: String, data: Map<String, Any>) {
        vendors.document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Vendor write failed", it) }
    }

    private fun DocumentSnapshot.toVendor(): Vendor? {
        val location = getGeoPoint("location") ?: return null
        return Vendor(id, getString("name"), location.latitude, location.longitude)
    }

    private companion object {
        const val TAG = "VendorRepository"
    }
}
