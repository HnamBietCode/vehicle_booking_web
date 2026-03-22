package com.example.driver_app.data.model

data class LoginResponse(
    val ok: Boolean,
    val message: String?,
    val token: String?,
    val userId: Long?,
    val role: String?
)
