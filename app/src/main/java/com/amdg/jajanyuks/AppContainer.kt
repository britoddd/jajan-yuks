package com.amdg.jajanyuks

import com.amdg.jajanyuks.data.VendorRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Manual dependency injection: one place that creates shared objects.
// Firebase is configured automatically from app/google-services.json.
class AppContainer {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val vendorRepository = VendorRepository(firestore)
}
