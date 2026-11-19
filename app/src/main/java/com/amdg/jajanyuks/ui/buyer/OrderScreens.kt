package com.amdg.jajanyuks.ui.buyer

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.ui.components.OrderLines
import com.amdg.jajanyuks.ui.components.formatRupiah
import com.amdg.jajanyuks.ui.components.orderStatusColor
import com.amdg.jajanyuks.ui.components.orderStatusLabel

@Composable
fun OrderScreen(onBack: () -> Unit, viewModel: OrderViewModel = viewModel(factory = OrderViewModel.Factory)) {
    val order by viewModel.order.collectAsStateWithLifecycle()
    val vendor by viewModel.vendor.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
        val o = order
        if (o == null) {
            item { Text(stringResource(R.string.loading)) }
            return@LazyColumn // nothing more to show until the order has loaded
        }
        item {
            Text(o.vendorName.ifBlank { stringResource(R.string.vendor_default_name) }, style = MaterialTheme.typography.headlineSmall)
            Text(orderStatusLabel(o.status), style = MaterialTheme.typography.titleLarge, color = orderStatusColor(o.status))
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { OrderLines(o) } } }

        // Walking directions to where the seller is right now, in Google Maps (free, no API key).
        val v = vendor
        if (o.status.isActive && v != null) {
            item {
                Button(
                    onClick = {
                        val uri = "https://www.google.com/maps/dir/?api=1&destination=${v.lat},${v.lng}&travelmode=walking".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.walk_to_vendor)) }
            }
        }
        if (o.status == OrderStatus.PENDING) {
            item {
                OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel_order))
                }
            }
        }
    }
}

@Composable
fun MyOrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: MyOrdersViewModel = viewModel(factory = MyOrdersViewModel.Factory),
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
        item { Text(stringResource(R.string.my_orders), style = MaterialTheme.typography.headlineSmall) }
        if (orders.isEmpty()) item { Text(stringResource(R.string.no_orders)) }
        items(orders, key = { it.id }) { order ->
            Card(Modifier.fillMaxWidth().clickable { onOpenOrder(order.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(order.vendorName.ifBlank { stringResource(R.string.vendor_default_name) }, style = MaterialTheme.typography.titleMedium)
                    Text(orderStatusLabel(order.status), color = orderStatusColor(order.status))
                    Text(formatRupiah(order.total))
                    order.createdAt?.let { Text(DateUtils.getRelativeTimeSpanString(it).toString(), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
