package com.example.driver_app.data.model

data class SoberBooking(
    val id: Long,
    val status: String? = null,
    val pickupLocation: String? = null,
    val dropoffLocation: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null
)
