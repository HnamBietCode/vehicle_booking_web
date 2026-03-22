package com.example.driver_app.data.model

data class VehicleRental(
    val id: Long,
    val status: String? = null,
    val pickupLocation: String? = null,
    val dropoffLocation: String? = null,
    val rentalMode: String? = null
)
