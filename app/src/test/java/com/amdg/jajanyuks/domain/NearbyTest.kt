package com.amdg.jajanyuks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyTest {
    private fun vendor(id: String, lat: Double, lng: Double) = Vendor(id, id, lat, lng)

    @Test
    fun distanceOfOneThousandthDegreeLatitudeIsAbout111m() {
        assertEquals(111.2, distanceMeters(-6.175, 106.827, -6.176, 106.827), 0.5)
    }

    @Test
    fun keepsOnlyVendorsWithin1kmClosestFirst() {
        val far = vendor("far", -6.200, 106.827)   // ~2.8 km away
        val near = vendor("near", -6.176, 106.827) // ~111 m
        val mid = vendor("mid", -6.180, 106.827)   // ~556 m
        val result = nearbyVendors(listOf(far, near, mid), -6.175, 106.827)
        assertEquals(listOf("near", "mid"), result.map { it.vendor.id })
    }

    @Test
    fun unknownBuyerLocationShowsAllWithoutDistance() {
        val result = nearbyVendors(listOf(vendor("a", 0.0, 0.0)), null, null)
        assertEquals(1, result.size)
        assertNull(result[0].distanceMeters)
    }

    @Test
    fun hidesOfflineAndStaleVendors() {
        val now = 100 * 60_000L
        val fresh = vendor("fresh", 0.0, 0.0).copy(updatedAt = now - 60_000)
        val stale = vendor("stale", 0.0, 0.0).copy(updatedAt = now - STALE_AFTER_MILLIS)
        val offline = vendor("off", 0.0, 0.0).copy(isLive = false)
        val result = nearbyVendors(listOf(fresh, stale, offline), 0.0, 0.0, now)
        assertEquals(listOf("fresh"), result.map { it.vendor.id })
    }
}
