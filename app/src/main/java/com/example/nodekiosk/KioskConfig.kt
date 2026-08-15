package com.example.nodekiosk

import android.content.Context
import java.net.URI
import java.security.MessageDigest

object KioskConfig {
    const val DEFAULT_SERVER_URL = "http://192.168.1.5:8000/"
    const val DEFAULT_ADMIN_PASSWORD_SHA256 = "5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5"
    private const val PREFS = "kiosk_settings"
    private const val URL_KEY = "server_url"
    private const val PASSWORD_KEY = "admin_password_hash"

    fun serverUrl(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(URL_KEY, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL

    fun saveServerUrl(context: Context, url: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(URL_KEY, url).apply()

    fun adminPasswordHash(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(PASSWORD_KEY, DEFAULT_ADMIN_PASSWORD_SHA256) ?: DEFAULT_ADMIN_PASSWORD_SHA256

    fun saveAdminPassword(context: Context, newPasswordRaw: String) {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(newPasswordRaw.toByteArray())
            .joinToString("") { "%02x".format(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PASSWORD_KEY, digest).apply()
    }

    fun validHttpUrl(value: String): Boolean = try {
        val uri = URI(value.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }
}

