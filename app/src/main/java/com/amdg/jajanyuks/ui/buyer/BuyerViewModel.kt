package com.amdg.jajanyuks.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.data.VendorRepository
import com.amdg.jajanyuks.domain.Vendor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BuyerViewModel(repository: VendorRepository) : ViewModel() {
    // `stateIn` keeps the latest list; the Firestore listener stops 5 s after the screen goes away.
    val vendors: StateFlow<List<Vendor>> = repository.liveVendors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        // Tells Compose's viewModel() how to build this ViewModel with its repository.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JajanYuksApp
                BuyerViewModel(app.container.vendorRepository)
            }
        }
    }
}
