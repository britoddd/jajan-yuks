package com.amdg.jajanyuks.ui.buyer

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.ui.components.formatRupiah

@Composable
fun VendorDetailScreen(onBack: () -> Unit, viewModel: VendorDetailViewModel = viewModel(factory = VendorDetailViewModel.Factory)) {
    val vendor by viewModel.vendor.collectAsStateWithLifecycle()
    val menu by viewModel.menu.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
        item {
            // Copy into a local `val` so Kotlin knows it can't turn null between the check and its use.
            val v = vendor
            if (v == null) {
                Text(stringResource(R.string.vendor_not_found))
            } else {
                Column {
                    Text(v.name ?: stringResource(R.string.vendor_default_name), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(if (v.isLive) R.string.vendor_live else R.string.vendor_offline),
                        color = if (v.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    v.updatedAt?.let {
                        Text(stringResource(R.string.updated_at, DateUtils.getRelativeTimeSpanString(it).toString()))
                    }
                }
            }
        }
        item { Text(stringResource(R.string.menu_title), style = MaterialTheme.typography.titleLarge) }
        if (menu.isEmpty()) item { Text(stringResource(R.string.vendor_menu_empty)) }
        items(menu, key = { it.id }) { BuyerMenuRow(it) }
    }
}

@Composable
private fun BuyerMenuRow(item: MenuItem) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(formatRupiah(item.price))
            }
            if (item.stock > 0) {
                Text(stringResource(R.string.stock_count, item.stock))
            } else {
                Text(stringResource(R.string.sold_out), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
