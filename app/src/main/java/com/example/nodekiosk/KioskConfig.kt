package com.example.nodekiosk

import android.content.Context
import android.util.Patterns
import java.net.URI

object KioskConfig {
    const val DEFAULT_SERVER_URL = "http://192.168.1.5:8000/"
    private const val PREFS = "kiosk_settings"
    private const val URL_KEY = "server_url"
    fun serverUrl(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    fun saveServerUrl(context: Context, url: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(URL_KEY, url).apply()
    fun validHttpUrl(value: String): Boolean = try {
        val uri = URI(value.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }
}
