package com.amdg.jajanyuks.ui.seller

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.location.LocationService

private val permissions = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    // Android 13+ needs permission to show the "you are live" notification.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
fun SellerScreen() {
    val context = LocalContext.current
    val serviceIntent = remember { Intent(context, LocationService::class.java) }
    // Reads the service's StateFlow as Compose state, pausing while the app is in the background.
    val isLive by LocationService.isRunning.collectAsStateWithLifecycle()
    var denied by remember { mutableStateOf(false) }

    // Shows the system permission dialog; the lambda gets the result.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        denied = !granted
        if (granted) ContextCompat.startForegroundService(context, serviceIntent)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(if (isLive) R.string.status_live else R.string.status_offline),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { if (isLive) context.stopService(serviceIntent) else launcher.launch(permissions) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (isLive) R.string.stop_live else R.string.go_live))
        }
        if (denied) Text(stringResource(R.string.permission_denied), color = MaterialTheme.colorScheme.error)
    }
}
