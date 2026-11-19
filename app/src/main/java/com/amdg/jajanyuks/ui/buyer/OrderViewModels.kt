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
import com.amdg.jajanyuks.OrderRoute
import com.amdg.jajanyuks.domain.Order
import com.amdg.jajanyuks.domain.OrderStatus
import com.amdg.jajanyuks.domain.Vendor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// One order, followed live, plus the seller's current position for walking directions.
class OrderViewModel(savedState: SavedStateHandle, private val container: AppContainer) : ViewModel() {
    private val orderId = savedState.toRoute<OrderRoute>().orderId

    val order: StateFlow<Order?> = container.orderRepository.order(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // `flatMapLatest` switches to listening to the order's vendor once the order has loaded.
    @OptIn(ExperimentalCoroutinesApi::class)
    val vendor: StateFlow<Vendor?> = order.map { it?.vendorId }.distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else container.vendorRepository.vendor(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun cancel() {
        if (order.value?.status == OrderStatus.PENDING) container.orderRepository.setStatus(orderId, OrderStatus.CANCELLED)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OrderViewModel(createSavedStateHandle(), (this[APPLICATION_KEY] as JajanYuksApp).container)
            }
        }
    }
}

class MyOrdersViewModel(container: AppContainer) : ViewModel() {
    val orders: StateFlow<List<Order>> = container.orderRepository.ordersForBuyer(container.auth.currentUser!!.uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { MyOrdersViewModel((this[APPLICATION_KEY] as JajanYuksApp).container) }
        }
    }
}
