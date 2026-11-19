package com.amdg.jajanyuks.ui.seller

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun SellerScreen(onLogout: () -> Unit, viewModel: SellerViewModel = viewModel(factory = SellerViewModel.Factory)) {
    val context = LocalContext.current
    val menu by viewModel.menu.collectAsStateWithLifecycle()
    val orders by viewModel.activeOrders.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
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

    // LazyColumn only builds the rows that are on screen, so long menus stay fast.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                stringResource(if (isLive) R.string.status_live else R.string.status_offline),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Button(
                onClick = { if (isLive) context.stopService(serviceIntent) else launcher.launch(permissions) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (isLive) R.string.stop_live else R.string.go_live))
            }
        }
        if (denied) item { Text(stringResource(R.string.permission_denied), color = MaterialTheme.colorScheme.error) }

        item { Text(stringResource(R.string.incoming_orders, orders.size), style = MaterialTheme.typography.titleLarge) }
        if (orders.isEmpty()) item { Text(stringResource(R.string.no_incoming_orders)) }
        items(orders, key = { "order-${it.id}" }) { order ->
            IncomingOrderCard(
                order = order,
                onAccept = { viewModel.accept(order) },
                onReject = { viewModel.reject(order) },
                onReady = { viewModel.markReady(order) },
                onComplete = { viewModel.complete(order) },
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.menu_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { adding = true }) { Text(stringResource(R.string.add_menu)) }
            }
        }
        if (menu.isEmpty()) item { Text(stringResource(R.string.menu_empty)) }
        // `key` lets Compose keep each row's state when the list changes order.
        items(menu, key = { it.id }) { item ->
            MenuItemRow(
                item = item,
                onChangeStock = { delta -> viewModel.changeStock(item, delta) },
                onDelete = { viewModel.delete(item) },
            )
        }

        // Hidden while live: stop first, so the seller is marked offline before signing out.
        if (!isLive) item { TextButton(onClick = onLogout) { Text(stringResource(R.string.logout)) } }
    }

    // One-time message, e.g. "not enough stock" when accepting an order.
    message?.let {
        AlertDialog(
            onDismissRequest = viewModel::messageShown,
            text = { Text(stringResource(it)) },
            confirmButton = { TextButton(onClick = viewModel::messageShown) { Text(stringResource(R.string.ok)) } },
        )
    }

    if (adding) {
        AddMenuItemDialog(
            onDismiss = { adding = false },
            onSave = { name, price, stock ->
                viewModel.addItem(name, price, stock)
                adding = false
            },
        )
    }
}
