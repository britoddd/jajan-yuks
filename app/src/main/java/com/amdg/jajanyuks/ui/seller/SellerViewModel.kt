package com.amdg.jajanyuks.ui.seller

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.R
import com.amdg.jajanyuks.data.OrderRepository
import com.amdg.jajanyuks.data.OutOfStockException
import com.amdg.jajanyuks.data.VendorRepository
import com.amdg.jajanyuks.data.VoiceParser
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.domain.VoiceUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// State of the voice dialog: waiting for Gemini, showing its proposal, or showing an error.
data class VoiceUiState(
    val thinking: Boolean = false,
    val proposal: VoiceUpdate? = null,
    @StringRes val error: Int? = null,
)

class SellerViewModel(
    private val uid: String,
    private val vendors: VendorRepository,
    private val orders: OrderRepository,
    private val voiceParser: VoiceParser,
) : ViewModel() {
    val menu: StateFlow<List<MenuItem>> = vendors.menu(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Only orders that still need the seller's attention, oldest first (first come, first served).
    val activeOrders: StateFlow<List<Order>> = orders.ordersForVendor(uid)
        .map { list -> list.filter { it.status.isActive }.reversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // A message to show once (e.g. "not enough stock"), as a string resource id.
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message
    fun messageShown() { _message.value = null }

    fun addItem(name: String, price: Long, stock: Int) = vendors.addMenuItem(uid, name.trim(), price, stock)

    fun changeStock(item: MenuItem, delta: Int) {
        if (item.stock + delta >= 0) vendors.changeStock(uid, item.id, delta)
    }

    fun delete(item: MenuItem) = vendors.deleteMenuItem(uid, item.id)

    fun accept(order: Order) = viewModelScope.launch {
        try {
            orders.accept(order)
        } catch (e: Exception) {
            Log.w("SellerViewModel", "Accept failed", e)
            // The transaction wraps our exception, so look at the cause too.
            showMessage(if (e is OutOfStockException || e.cause is OutOfStockException) R.string.not_enough_stock else R.string.error_network)
        }
    }

    fun reject(order: Order) = orders.setStatus(order.id, OrderStatus.CANCELLED)
    fun markReady(order: Order) = orders.setStatus(order.id, OrderStatus.READY)
    fun complete(order: Order) = orders.setStatus(order.id, OrderStatus.COMPLETED)

    private fun showMessage(@StringRes message: Int) { _message.value = message }

    // --- Voice update: text → Gemini → proposal → seller confirms → saved ---

    private val _voice = MutableStateFlow(VoiceUiState())
    val voice: StateFlow<VoiceUiState> = _voice

    fun parseVoice(text: String) = viewModelScope.launch {
        _voice.value = VoiceUiState(thinking = true)
        _voice.value = try {
            VoiceUiState(proposal = voiceParser.parse(text, menu.value))
        } catch (e: CancellationException) {
            throw e // the screen closed; let the coroutine stop normally
        } catch (e: Exception) {
            Log.w("SellerViewModel", "Voice parse failed", e)
            VoiceUiState(error = R.string.voice_failed)
        }
    }

    fun applyVoice() {
        val proposal = _voice.value.proposal ?: return
        proposal.stock.forEach { vendors.setStock(uid, it.item.id, it.newStock) }
        proposal.statusText?.let { vendors.setStatusText(uid, it) }
        resetVoice()
    }

    fun voiceUnavailable() { _voice.value = VoiceUiState(error = R.string.voice_unavailable) }
    fun resetVoice() { _voice.value = VoiceUiState() }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as JajanYuksApp).container
                // The seller screen is only reachable when signed in, so `!!` (assert not null) is safe here.
                SellerViewModel(
                    container.auth.currentUser!!.uid,
                    container.vendorRepository,
                    container.orderRepository,
                    container.voiceParser,
                )
            }
        }
    }
}
