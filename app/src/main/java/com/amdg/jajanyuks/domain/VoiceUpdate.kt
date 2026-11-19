package com.amdg.jajanyuks.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// What Gemini understood from the seller's sentence. Nothing is saved until the seller confirms.
data class VoiceUpdate(val stock: List<StockChange>, val statusText: String?) {
    val isEmpty get() = stock.isEmpty() && statusText == null
}

data class StockChange(val item: MenuItem, val newStock: Int)

// The JSON shape Gemini is asked to return (see VoiceParser). @Serializable lets kotlinx.serialization
// turn JSON text into these classes; the defaults are used when a field is missing.
@Serializable private data class VoiceJson(val stock: List<StockJson> = emptyList(), val statusText: String? = null)
@Serializable private data class StockJson(val itemId: String, val stock: Int)

private val json = Json { ignoreUnknownKeys = true }

const val MAX_STATUS_LENGTH = 80

// AI output is untrusted: unknown item ids, negative stock and "no change" entries are dropped.
// Throws if the text is not valid JSON.
fun parseVoiceUpdate(raw: String, menu: List<MenuItem>): VoiceUpdate {
    val parsed = json.decodeFromString<VoiceJson>(raw)
    val byId = menu.associateBy { it.id }
    val stock = parsed.stock.mapNotNull { s ->
        val item = byId[s.itemId] ?: return@mapNotNull null
        if (s.stock < 0 || s.stock == item.stock) null else StockChange(item, s.stock)
    }.distinctBy { it.item.id }
    val status = parsed.statusText?.trim()?.take(MAX_STATUS_LENGTH)?.ifBlank { null }
    return VoiceUpdate(stock, status)
}
