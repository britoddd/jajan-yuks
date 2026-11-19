package com.amdg.jajanyuks.ui.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.ui.components.formatRupiah

// One row of the seller's menu: name, price, and stock with -/+ buttons.
@Composable
fun MenuItemRow(item: MenuItem, onChangeStock: (Int) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // weight(1f) = take all the space left over after the buttons.
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(formatRupiah(item.price), style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = { onChangeStock(-1) }, enabled = item.stock > 0) { Text("−") }
            Text(
                stringResource(R.string.stock_count, item.stock),
                modifier = Modifier.padding(horizontal = 8.dp),
                color = if (item.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(onClick = { onChangeStock(1) }) { Text("+") }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
        }
    }
}

@Composable
fun AddMenuItemDialog(onDismiss: () -> Unit, onSave: (name: String, price: Long, stock: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_menu)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.menu_name)) }, singleLine = true)
                NumberField(price, { price = it }, stringResource(R.string.menu_price))
                NumberField(stock, { stock = it }, stringResource(R.string.menu_stock))
                if (invalid) Text(stringResource(R.string.invalid_menu), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // `toLongOrNull` returns null instead of crashing when the text isn't a number.
                val p = price.toLongOrNull()
                val s = stock.toIntOrNull()
                if (name.isBlank() || p == null || p <= 0 || s == null || s < 0) invalid = true else onSave(name, p, s)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
