package com.amdg.jajanyuks.ui.buyer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.amdg.jajanyuks.AppContainer
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.VendorDetailRoute
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.domain.OrderLine
import com.amdg.jajanyuks.domain.Vendor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VendorDetailViewModel(savedState: SavedStateHandle, private val container: AppContainer) : ViewModel() {
    // The vendor id comes from the navigation route (VendorDetailRoute).
    private val vendorId = savedState.toRoute<VendorDetailRoute>().vendorId

    val vendor: StateFlow<Vendor?> = container.vendorRepository.vendor(vendorId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val menu: StateFlow<List<MenuItem>> = container.vendorRepository.menu(vendorId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Cart: menu item id -> quantity the buyer wants.
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart

    private val _placing = MutableStateFlow(false)
    val placing: StateFlow<Boolean> = _placing

    // Quantity stays between 0 and the stock left.
    fun changeQty(item: MenuItem, delta: Int) = _cart.update { cart ->
        val qty = ((cart[item.id] ?: 0) + delta).coerceIn(0, item.stock)
        if (qty == 0) cart - item.id else cart + (item.id to qty)
    }

    fun placeOrder(onPlaced: (orderId: String) -> Unit) {
        val lines = menu.value.mapNotNull { item ->
            _cart.value[item.id]?.let { qty -> OrderLine(item.id, item.name, item.price, qty) }
        }
        val buyerId = container.auth.currentUser?.uid ?: return
        if (lines.isEmpty() || _placing.value) return
        _placing.value = true
        viewModelScope.launch {
            val buyerName = runCatching { container.userRepository.currentName() }.getOrNull() ?: ""
            val orderId = container.orderRepository.placeOrder(vendorId, vendor.value?.name ?: "", buyerId, buyerName, lines)
            _cart.value = emptyMap()
            _placing.value = false
            onPlaced(orderId)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JajanYuksApp
                VendorDetailViewModel(createSavedStateHandle(), app.container)
            }
        }
    }
}
