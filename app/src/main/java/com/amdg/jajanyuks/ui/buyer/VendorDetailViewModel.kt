package com.amdg.jajanyuks.ui.buyer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.VendorDetailRoute
import com.amdg.jajanyuks.data.VendorRepository
import com.amdg.jajanyuks.domain.MenuItem
import com.amdg.jajanyuks.domain.Vendor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class VendorDetailViewModel(savedState: SavedStateHandle, repository: VendorRepository) : ViewModel() {
    // The vendor id comes from the navigation route (VendorDetailRoute).
    private val vendorId = savedState.toRoute<VendorDetailRoute>().vendorId

    val vendor: StateFlow<Vendor?> = repository.vendor(vendorId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val menu: StateFlow<List<MenuItem>> = repository.menu(vendorId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JajanYuksApp
                VendorDetailViewModel(createSavedStateHandle(), app.container.vendorRepository)
            }
        }
    }
}
