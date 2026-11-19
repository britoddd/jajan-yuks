package com.amdg.jajanyuks

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Manual dependency injection: one place that creates shared objects.
// Firebase is configured automatically from app/google-services.json.
// Repositories will be added here as features are built.
class AppContainer {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
}
