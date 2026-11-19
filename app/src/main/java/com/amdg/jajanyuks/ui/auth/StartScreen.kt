package com.amdg.jajanyuks.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.amdg.jajanyuks.data.UserRepository
import com.amdg.jajanyuks.domain.Role

// First screen: decides where to go based on who is signed in.
// role == null means "not signed in" (or no profile), so the user goes to login.
@Composable
fun StartScreen(users: UserRepository, onResult: (Role?) -> Unit) {
    LaunchedEffect(Unit) {
        val role = runCatching { users.currentRole() }.getOrNull()
        if (role == null) users.signOut()
        onResult(role)
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
