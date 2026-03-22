package com.example.driver_app.service

import com.example.driver_app.data.repo.DriverJobsRepository
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DriverFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            DriverJobsRepository.registerDevice(token)
        }
    }
}
