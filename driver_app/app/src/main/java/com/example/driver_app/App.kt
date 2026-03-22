package com.example.driver_app

import android.app.Application
import com.example.driver_app.data.store.TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
