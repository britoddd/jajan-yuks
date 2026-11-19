package com.amdg.jajanyuks

import android.app.Application
import com.amdg.jajanyuks.ui.buyer.OrderReadyNotifier
import org.maplibre.android.MapLibre

// Created once when the app starts; holds the AppContainer for the whole app.
// `lateinit` means "assigned later, before first use" (here: in onCreate).
class JajanYuksApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this) // must run before any MapView is created
        container = AppContainer()
        OrderReadyNotifier(this, container.auth, container.orderRepository).start()
    }
}
