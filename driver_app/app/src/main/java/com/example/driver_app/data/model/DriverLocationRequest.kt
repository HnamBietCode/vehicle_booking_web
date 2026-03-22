package com.example.driver_app.data.model

data class DriverLocationRequest(
    val tripType: String, // SOBER or RENTAL
    val tripId: Long,
    val lat: Double,
    val lng: Double,
    val speed: Double? = null,
    val bearing: Double? = null,
    val accuracy: Double? = null,
    val recordedAt: String? = null
)
