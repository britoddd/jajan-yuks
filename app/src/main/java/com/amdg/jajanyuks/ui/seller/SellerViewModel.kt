package com.amdg.jajanyuks.ui.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.data.VendorRepository
import com.amdg.jajanyuks.domain.MenuItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SellerViewModel(private val uid: String, private val vendors: VendorRepository) : ViewModel() {
    val menu: StateFlow<List<MenuItem>> = vendors.menu(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(name: String, price: Long, stock: Int) = vendors.addMenuItem(uid, name.trim(), price, stock)

    fun changeStock(item: MenuItem, delta: Int) {
        if (item.stock + delta >= 0) vendors.changeStock(uid, item.id, delta)
    }

    fun delete(item: MenuItem) = vendors.deleteMenuItem(uid, item.id)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as JajanYuksApp).container
                // The seller screen is only reachable when signed in, so `!!` (assert not null) is safe here.
                SellerViewModel(container.auth.currentUser!!.uid, container.vendorRepository)
            }
        }
    }
}
