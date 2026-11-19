package com.amdg.jajanyuks.data

import android.util.Log
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.domain.Vendor
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// The vendor document id is the seller's auth uid, so Security Rules can check ownership easily.
// Writes are fire-and-forget: Firestore applies them locally at once (listeners update the UI)
// and sends them to the server when online.
class VendorRepository(firestore: FirebaseFirestore) {
    private val vendors = firestore.collection("vendors")
    private fun menuOf(vendorId: String) = vendors.document(vendorId).collection("menu")

    // --- Live location ---

    fun updateLocation(uid: String, lat: Double, lng: Double) {
        val data = mapOf(
            "ownerId" to uid,
            "isLive" to true,
            "location" to GeoPoint(lat, lng),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        vendors.document(uid).set(data, SetOptions.merge()).logFailure()
    }

    fun goOffline(uid: String) {
        val data = mapOf("ownerId" to uid, "isLive" to false, "updatedAt" to FieldValue.serverTimestamp())
        vendors.document(uid).set(data, SetOptions.merge()).logFailure()
    }

    // All live vendors; the 1 km filter happens in `nearbyVendors` on the phone.
    fun liveVendors(): Flow<List<Vendor>> = vendors.whereEqualTo("isLive", true).listen { it.toVendor() }

    // One vendor, updated live (null if missing or never went live).
    fun vendor(vendorId: String): Flow<Vendor?> = callbackFlow {
        val registration = vendors.document(vendorId).addSnapshotListener { snapshot, error ->
            if (error != null) Log.w(TAG, "Vendor listener failed", error)
            trySend(snapshot?.toVendor())
        }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toVendor(): Vendor? {
        val location = getGeoPoint("location") ?: return null
        return Vendor(
            id = id,
            name = getString("name"),
            lat = location.latitude,
            lng = location.longitude,
            isLive = getBoolean("isLive") == true,
            updatedAt = getTimestamp("updatedAt")?.toDate()?.time,
        )
    }

    // --- Menu (vendors/{vendorId}/menu/{itemId}) ---

    fun menu(vendorId: String): Flow<List<MenuItem>> = menuOf(vendorId).orderBy("name").listen { doc ->
        MenuItem(
            id = doc.id,
            name = doc.getString("name") ?: return@listen null,
            price = doc.getLong("price") ?: 0,
            stock = (doc.getLong("stock") ?: 0).toInt(),
        )
    }

    fun addMenuItem(vendorId: String, name: String, price: Long, stock: Int) {
        menuOf(vendorId).add(mapOf("name" to name, "price" to price, "stock" to stock)).logFailure()
    }

    // `increment` adds on the server, so two quick taps never overwrite each other.
    fun changeStock(vendorId: String, itemId: String, delta: Int) {
        menuOf(vendorId).document(itemId).update("stock", FieldValue.increment(delta.toLong())).logFailure()
    }

    fun deleteMenuItem(vendorId: String, itemId: String) {
        menuOf(vendorId).document(itemId).delete().logFailure()
    }

    // --- Helpers ---

    // `callbackFlow` turns a callback-based snapshot listener into a Flow.
    // The listener is removed automatically when nobody collects anymore (awaitClose).
    private fun <T : Any> Query.listen(map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (snapshot == null) {
                Log.w(TAG, "Listener failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot.documents.mapNotNull(map))
        }
        awaitClose { registration.remove() }
    }

    private fun <T> Task<T>.logFailure() {
        addOnFailureListener { Log.w(TAG, "Write failed", it) }
    }

    private companion object {
        const val TAG = "VendorRepository"
    }
}
