package com.amdg.jajanyuks.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amdg.jajanyuks.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

// Sprint 1 placeholder: signs in anonymously and lets the user pick a role.
// Email sign-up with a saved role replaces this in sprint 2.
@Composable
fun RoleScreen(auth: FirebaseAuth, onSeller: () -> Unit, onBuyer: () -> Unit) {
    // `remember { mutableStateOf(..) }` keeps a value across recompositions;
    // changing it redraws the parts of the UI that read it.
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var failed by remember { mutableStateOf(false) }

    // Runs once when the screen appears; `await()` waits for the Firebase task without blocking the UI.
    LaunchedEffect(Unit) {
        if (!signedIn) {
            signedIn = runCatching { auth.signInAnonymously().await() }
                .onFailure { Log.w("RoleScreen", "Anonymous sign-in failed", it) }
                .isSuccess
            failed = !signedIn
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onSeller, enabled = signedIn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.role_seller))
        }
        OutlinedButton(onClick = onBuyer, enabled = signedIn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.role_buyer))
        }
        if (failed) Text(stringResource(R.string.sign_in_failed), color = MaterialTheme.colorScheme.error)
    }
}
