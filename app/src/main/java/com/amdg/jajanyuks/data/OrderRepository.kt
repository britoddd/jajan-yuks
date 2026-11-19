package com.amdg.jajanyuks.data

import android.util.Log
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderLine
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.domain.missingStock
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OutOfStockException(val items: List<OrderLine>) : Exception("Not enough stock")

class OrderRepository(private val firestore: FirebaseFirestore) {
    private val orders = firestore.collection("orders")
    private fun menuItem(vendorId: String, itemId: String) =
        firestore.collection("vendors").document(vendorId).collection("menu").document(itemId)

    // Returns the new order's id right away; Firestore sends the write in the background.
    fun placeOrder(vendorId: String, vendorName: String, buyerId: String, buyerName: String, lines: List<OrderLine>): String {
        val doc = orders.document()
        val data = mapOf(
            "vendorId" to vendorId,
            "vendorName" to vendorName,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "items" to lines.map { mapOf("itemId" to it.itemId, "name" to it.name, "price" to it.price, "qty" to it.qty) },
            "status" to OrderStatus.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        doc.set(data).addOnFailureListener { Log.w(TAG, "Place order failed", it) }
        return doc.id
    }

    // A transaction reads the stock and writes the new stock + status as one step, so two
    // orders accepted at the same moment can never sell the same last item twice.
    suspend fun accept(order: Order) {
        firestore.runTransaction { tx ->
            val stock = order.lines.associate { line ->
                line.itemId to (tx.get(menuItem(order.vendorId, line.itemId)).getLong("stock")?.toInt() ?: 0)
            }
            val missing = missingStock(order.lines, stock)
            if (missing.isNotEmpty()) throw OutOfStockException(missing)
            order.lines.forEach { line ->
                tx.update(menuItem(order.vendorId, line.itemId), "stock", stock.getValue(line.itemId) - line.qty)
            }
            tx.update(orders.document(order.id), statusChange(OrderStatus.ACCEPTED))
        }.await()
    }

    fun setStatus(orderId: String, status: OrderStatus) {
        orders.document(orderId).update(statusChange(status))
            .addOnFailureListener { Log.w(TAG, "Status change failed", it) }
    }

    // ponytail: no orderBy/status filter in the query (avoids a composite index); sorted on the phone.
    fun ordersForVendor(vendorId: String): Flow<List<Order>> = listOrders("vendorId", vendorId)
    fun ordersForBuyer(buyerId: String): Flow<List<Order>> = listOrders("buyerId", buyerId)

    fun order(orderId: String): Flow<Order?> = callbackFlow {
        val registration = orders.document(orderId).addSnapshotListener { snapshot, error ->
            if (error != null) Log.w(TAG, "Order listener failed", error)
            trySend(snapshot?.toOrder())
        }
        awaitClose { registration.remove() }
    }

    private fun listOrders(field: String, uid: String): Flow<List<Order>> = callbackFlow {
        val registration = orders.whereEqualTo(field, uid).addSnapshotListener { snapshot, error ->
            if (snapshot == null) {
                Log.w(TAG, "Orders listener failed", error)
                return@addSnapshotListener
            }
            trySend(snapshot.documents.mapNotNull { it.toOrder() }.sortedByDescending { it.createdAt ?: Long.MAX_VALUE })
        }
        awaitClose { registration.remove() }
    }

    private fun statusChange(status: OrderStatus) =
        mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp())

    private fun DocumentSnapshot.toOrder(): Order? {
        if (!exists()) return null
        // Firestore gives arrays of maps back as List<Map<String, Any>>; `as?` is a safe cast (null if wrong type).
        val items = (get("items") as? List<*>).orEmpty().mapNotNull { raw ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            OrderLine(
                itemId = m["itemId"] as? String ?: return@mapNotNull null,
                name = m["name"] as? String ?: "",
                price = (m["price"] as? Number)?.toLong() ?: 0,
                qty = (m["qty"] as? Number)?.toInt() ?: 0,
            )
        }
        return Order(
            id = id,
            vendorId = getString("vendorId") ?: return null,
            vendorName = getString("vendorName") ?: "",
            buyerId = getString("buyerId") ?: return null,
            buyerName = getString("buyerName") ?: "",
            lines = items,
            status = OrderStatus.entries.find { it.name == getString("status") } ?: OrderStatus.PENDING,
            createdAt = getTimestamp("createdAt")?.toDate()?.time,
        )
    }

    private companion object {
        const val TAG = "OrderRepository"
    }
}
