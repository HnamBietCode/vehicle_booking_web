//package com.example.driver_app.data.api
//
//interface ApiService {
//    @POST("api/mobile/auth/login")
//    suspend fun login(@Body body: LoginRequest): LoginResponse
//
//    @GET("api/driver/sober-bookings/pending")
//    suspend fun getPendingSober(): List<SoberBooking>
//
//    @POST("api/driver/sober-bookings/{id}/accept")
//    suspend fun acceptSober(@Path("id") id: Long): Response<String>
//
//    @GET("api/driver/rentals/pending-vehicle-only")
//    suspend fun getPendingVehicleOnly(): List<VehicleRental>
//
//    @POST("api/driver/rentals/{id}/accept")
//    suspend fun acceptRental(@Path("id") id: Long): Response<String>
//
//    @POST("api/driver/locations")
//    suspend fun updateLocation(@Body body: DriverLocationRequest): Response<Unit>
//}
