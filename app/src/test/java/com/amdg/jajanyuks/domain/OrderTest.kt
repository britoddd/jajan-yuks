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

    @Test
    fun newlyReadyOnlyAnnouncesTransitions() {
        fun order(id: String, status: OrderStatus) = Order(id, "v", "V", "b", "B", listOf(cilok), status, null)
        val ready = order("1", OrderStatus.READY)
        assertEquals(emptyList<Order>(), newlyReady(null, listOf(ready))) // first snapshot = baseline
        assertEquals(listOf(ready), newlyReady(mapOf("1" to OrderStatus.ACCEPTED), listOf(ready)))
        assertEquals(emptyList<Order>(), newlyReady(mapOf("1" to OrderStatus.READY), listOf(ready))) // already told
    }
}
