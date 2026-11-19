package com.amdg.jajanyuks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceUpdateTest {
    private val cilok = MenuItem("c1", "Cilok Kuah", 5000, 7)
    private val esTeh = MenuItem("e1", "Es Teh", 3000, 4)

    @Test
    fun keepsOnlyRealChanges() {
        val raw = """{"stock":[{"itemId":"c1","stock":5},{"itemId":"e1","stock":4},{"itemId":"zzz","stock":3},{"itemId":"c1","stock":-1}],"statusText":"  Lagi di depan SD  "}"""
        val update = parseVoiceUpdate(raw, listOf(cilok, esTeh))
        // Unknown id, unchanged stock and negative stock are dropped.
        assertEquals(listOf(StockChange(cilok, 5)), update.stock)
        assertEquals("Lagi di depan SD", update.statusText)
    }

    @Test
    fun missingFieldsMeanNoChange() {
        val update = parseVoiceUpdate("""{"statusText":" "}""", listOf(cilok))
        assertEquals(emptyList<StockChange>(), update.stock)
        assertNull(update.statusText)
        assert(update.isEmpty)
    }
}
