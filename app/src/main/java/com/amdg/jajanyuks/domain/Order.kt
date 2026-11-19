package com.amdg.jajanyuks.domain

// Orders that turned READY since the previous snapshot. `before == null` (first snapshot)
// only records the baseline, so opening the app doesn't re-announce old ready orders.
fun newlyReady(before: Map<String, OrderStatus>?, orders: List<Order>): List<Order> =
    if (before == null) emptyList()
    else orders.filter { it.status == OrderStatus.READY && before[it.id] != OrderStatus.READY }

// Lifecycle: PENDING -> ACCEPTED -> READY -> COMPLETED. A PENDING order can be CANCELLED
// (seller rejects it, or buyer cancels). Stock is taken when the seller accepts.
enum class OrderStatus { PENDING, ACCEPTED, READY, COMPLETED, CANCELLED;

    val isActive get() = this == PENDING || this == ACCEPTED || this == READY
}

data class OrderLine(val itemId: String, val name: String, val price: Long, val qty: Int)

data class Order(
    val id: String,
    val vendorId: String,
    val vendorName: String,
    val buyerId: String,
    val buyerName: String,
    val lines: List<OrderLine>,
    val status: OrderStatus,
    val createdAt: Long?, // epoch millis
) {
    val total: Long get() = lines.sumOf { it.price * it.qty }
}

// Lines the seller can't fulfil with the current stock (itemId -> stock left). Empty = OK to accept.
fun missingStock(lines: List<OrderLine>, stock: Map<String, Int>): List<OrderLine> =
    lines.filter { (stock[it.itemId] ?: 0) < it.qty }
