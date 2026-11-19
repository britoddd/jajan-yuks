package com.amdg.jajanyuks.ui.components

import java.text.NumberFormat
import java.util.Locale

private val rupiah = NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID"))

// 15000 -> "Rp15.000" (Indonesian uses "." as the thousands separator).
fun formatRupiah(amount: Long): String = "Rp" + rupiah.format(amount)
