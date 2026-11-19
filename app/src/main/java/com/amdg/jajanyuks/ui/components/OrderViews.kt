package com.amdg.jajanyuks.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderStatus

@Composable
fun orderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.PENDING -> R.string.status_pending
        OrderStatus.ACCEPTED -> R.string.status_accepted
        OrderStatus.READY -> R.string.status_ready
        OrderStatus.COMPLETED -> R.string.status_completed
        OrderStatus.CANCELLED -> R.string.status_cancelled
    },
)

@Composable
fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.READY -> MaterialTheme.colorScheme.primary
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

// "2x Cilok Kuah   Rp10.000" per line, then the total.
@Composable
fun OrderLines(order: Order) {
    Column {
        order.lines.forEach { line ->
            Row(Modifier.fillMaxWidth()) {
                Text("${line.qty}x ${line.name}", Modifier.weight(1f))
                Text(formatRupiah(line.price * line.qty))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.total), Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(formatRupiah(order.total), fontWeight = FontWeight.Bold)
        }
    }
}
