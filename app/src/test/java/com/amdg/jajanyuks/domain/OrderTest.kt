package com.amdg.jajanyuks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTest {
    private val cilok = OrderLine("c", "Cilok", 5_000, 3)
    private val teh = OrderLine("t", "Es Teh", 3_000, 2)

    @Test
    fun totalSumsPriceTimesQty() {
        val order = Order("o", "v", "V", "b", "B", listOf(cilok, teh), OrderStatus.PENDING, null)
        assertEquals(21_000, order.total)
    }

    @Test
    fun missingStockListsLinesThatExceedStock() {
        assertEquals(emptyList<OrderLine>(), missingStock(listOf(cilok, teh), mapOf("c" to 3, "t" to 5)))
        assertEquals(listOf(teh), missingStock(listOf(cilok, teh), mapOf("c" to 10, "t" to 1)))
        assertEquals(listOf(cilok), missingStock(listOf(cilok), emptyMap())) // item deleted
    }

    @Test
    fun onlyOpenOrdersAreActive() {
        assertTrue(OrderStatus.PENDING.isActive)
        assertTrue(OrderStatus.READY.isActive)
        assertFalse(OrderStatus.COMPLETED.isActive)
        assertFalse(OrderStatus.CANCELLED.isActive)
    }
}
