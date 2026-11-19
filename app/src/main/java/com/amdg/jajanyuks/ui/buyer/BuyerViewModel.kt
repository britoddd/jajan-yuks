package com.amdg.jajanyuks.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.amdg.jajanyuks.JajanYuksApp
import com.amdg.jajanyuks.data.VendorRepository
import com.amdg.jajanyuks.domain.NearbyVendor
import com.amdg.jajanyuks.domain.nearbyVendors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LatLng(val lat: Double, val lng: Double)

class BuyerViewModel(repository: VendorRepository) : ViewModel() {
    private val _myLocation = MutableStateFlow<LatLng?>(null)
    val myLocation: StateFlow<LatLng?> = _myLocation

    // `combine` recomputes the list whenever either the vendors or the buyer's location changes.
    val nearby: StateFlow<List<NearbyVendor>> = combine(repository.liveVendors(), _myLocation) { vendors, me ->
        nearbyVendors(vendors, me?.lat, me?.lng)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateMyLocation(lat: Double, lng: Double) {
        _myLocation.value = LatLng(lat, lng)
    }

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
