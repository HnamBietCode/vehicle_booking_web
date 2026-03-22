package com.example.driver_app.data.api

import com.example.driver_app.BuildConfig
import com.example.driver_app.data.store.TokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = TokenStore.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .build()

    private fun baseUrl(): String = TokenStore.getBaseUrl() ?: BuildConfig.API_BASE_URL

    val api: DriverApi
        get() = Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DriverApi::class.java)
}
