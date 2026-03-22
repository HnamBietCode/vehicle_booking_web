package com.example.driver_app.data.api

import com.example.driver_app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface DriverApi {

    @POST("/api/mobile/auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @POST("/api/mobile/devices/register")
    suspend fun registerDevice(@Body req: DeviceTokenRequest): Response<Unit>

    @GET("/api/driver/sober-bookings/pending")
    suspend fun pendingSober(): List<SoberBooking>

    @GET("/api/driver/sober-bookings/assigned")
    suspend fun assignedSober(): List<SoberBooking>

    @POST("/api/driver/sober-bookings/{id}/accept")
    suspend fun acceptSober(@Path("id") id: Long): Response<String>

    @POST("/api/driver/sober-bookings/{id}/arrive")
    suspend fun arriveSober(@Path("id") id: Long): Response<String>

    @POST("/api/driver/sober-bookings/{id}/start")
    suspend fun startSober(@Path("id") id: Long): Response<String>

    @POST("/api/driver/sober-bookings/{id}/complete")
    suspend fun completeSober(@Path("id") id: Long): Response<String>

    @GET("/api/driver/rentals/pending-all")
    suspend fun pendingAllRentals(): List<VehicleRental>

    @GET("/api/driver/rentals/assigned")
    suspend fun assignedRentals(): List<VehicleRental>

    @POST("/api/driver/rentals/{id}/accept")
    suspend fun acceptRental(@Path("id") id: Long): Response<String>

    @POST("/api/driver/rentals/{id}/start")
    suspend fun startRental(@Path("id") id: Long): Response<String>

    @POST("/api/driver/rentals/{id}/complete")
    suspend fun completeRental(@Path("id") id: Long): Response<String>

    @POST("/api/driver/locations")
    suspend fun sendLocation(@Body req: DriverLocationRequest): Response<Unit>
}
