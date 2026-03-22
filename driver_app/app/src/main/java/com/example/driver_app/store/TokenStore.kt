package com.example.driver_app.data.store

import android.content.Context
import android.content.SharedPreferences

object TokenStore {
    private var prefs: SharedPreferences? = null
    private const val TOKEN_KEY = "token"
    private const val BASE_URL_KEY = "base_url"

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("driver_app", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs?.edit()?.putString(TOKEN_KEY, token)?.apply()
    }

    fun getToken(): String? = prefs?.getString(TOKEN_KEY, null)

    fun saveBaseUrl(baseUrl: String) {
        prefs?.edit()?.putString(BASE_URL_KEY, normalizeBaseUrl(baseUrl))?.apply()
    }

    fun getBaseUrl(): String? = prefs?.getString(BASE_URL_KEY, null)

    fun clear() {
        prefs?.edit()?.remove(TOKEN_KEY)?.apply()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
