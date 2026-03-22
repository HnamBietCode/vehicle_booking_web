package com.example.driver_app.location

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DriverLocationService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
