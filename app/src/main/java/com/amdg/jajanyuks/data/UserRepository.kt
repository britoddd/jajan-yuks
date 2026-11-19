package com.amdg.jajanyuks.data

import com.amdg.jajanyuks.domain.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// `suspend` functions can pause (e.g. wait for the network) without blocking the UI thread.
class UserRepository(private val auth: FirebaseAuth, firestore: FirebaseFirestore) {
    private val users = firestore.collection("users")
    private val vendors = firestore.collection("vendors")

    suspend fun register(name: String, email: String, password: String, role: Role) {
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user!!.uid
        users.document(uid).set(mapOf("name" to name, "email" to email, "role" to role.name)).await()
        if (role == Role.SELLER) {
            // Create the vendor document now so buyers see the seller's name once they go live.
            vendors.document(uid)
                .set(mapOf("ownerId" to uid, "name" to name, "isLive" to false), SetOptions.merge())
                .await()
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    // null = not signed in, or the account has no profile (e.g. an old anonymous test user).
    suspend fun currentRole(): Role? {
        val uid = auth.currentUser?.uid ?: return null
        val role = users.document(uid).get().await().getString("role")
        return Role.entries.find { it.name == role }
    }

    fun signOut() = auth.signOut()
}
