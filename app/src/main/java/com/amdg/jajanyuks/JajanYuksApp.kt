package com.amdg.jajanyuks

import android.app.Application

// Created once when the app starts; holds the AppContainer for the whole app.
// `lateinit` means "assigned later, before first use" (here: in onCreate).
class JajanYuksApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
