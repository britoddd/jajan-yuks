package com.amdg.jajanyuks.ui.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.ui.components.OrderLines
import com.amdg.jajanyuks.ui.components.orderStatusColor
import com.amdg.jajanyuks.ui.components.orderStatusLabel

// One incoming order with the next action the seller can take.
@Composable
fun IncomingOrderCard(order: Order, onAccept: () -> Unit, onReject: () -> Unit, onReady: () -> Unit, onComplete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(order.buyerName.ifBlank { stringResource(R.string.buyer_default_name) }, style = MaterialTheme.typography.titleMedium)
            Text(orderStatusLabel(order.status), color = orderStatusColor(order.status))
            OrderLines(order)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (order.status) {
                    OrderStatus.PENDING -> {
                        Button(onClick = onAccept) { Text(stringResource(R.string.accept)) }
                        OutlinedButton(onClick = onReject) { Text(stringResource(R.string.reject)) }
                    }
                    OrderStatus.ACCEPTED -> Button(onClick = onReady) { Text(stringResource(R.string.mark_ready)) }
                    OrderStatus.READY -> Button(onClick = onComplete) { Text(stringResource(R.string.mark_completed)) }
                    else -> Unit
                }
            }
        }
    }
}
