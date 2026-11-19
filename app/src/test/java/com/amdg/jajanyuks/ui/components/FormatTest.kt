package com.amdg.jajanyuks.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun formatsRupiahWithDots() {
        assertEquals("Rp0", formatRupiah(0))
        assertEquals("Rp5.000", formatRupiah(5_000))
        assertEquals("Rp1.250.000", formatRupiah(1_250_000))
    }

    @Test
    fun formatsDistanceInMetersOrKilometers() {
        assertEquals("350 m", formatDistance(350.4))
        assertEquals("1,2 km", formatDistance(1_234.0))
    }
}
