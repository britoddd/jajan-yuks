package com.amdg.jajanyuks.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val indonesian = Locale.forLanguageTag("id-ID")
private val rupiah = NumberFormat.getIntegerInstance(indonesian)

// 15000 -> "Rp15.000" (Indonesian uses "." as the thousands separator).
fun formatRupiah(amount: Long): String = "Rp" + rupiah.format(amount)

// 350.4 -> "350 m", 1234.0 -> "1,2 km" (Indonesian uses "," for decimals).
fun formatDistance(meters: Double): String =
    if (meters < 1_000) "${meters.roundToInt()} m" else String.format(indonesian, "%.1f km", meters / 1_000)
