package com.amdg.jajanyuks.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.amdg.jajanyuks.data.UserRepository
import com.amdg.jajanyuks.domain.Role
import kotlinx.coroutines.delay

// First screen: decides where to go based on who is signed in.
// role == null means "not signed in" (or no profile), so the user goes to login.
@Composable
fun StartScreen(users: UserRepository, onResult: (Role?) -> Unit) {
    LaunchedEffect(Unit) {
        // A network hiccup must not log the user out: retry, and only sign out when the
        // profile was really read and is missing.
        repeat(3) { attempt ->
            val result = runCatching { users.currentRole() }
            if (result.isSuccess) {
                val role = result.getOrNull()
                if (role == null) users.signOut()
                return@LaunchedEffect onResult(role)
            }
            Log.w("StartScreen", "Loading role failed (attempt ${attempt + 1})", result.exceptionOrNull())
            delay(2_000)
        }
        onResult(null) // still failing: show login, but keep the session
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
