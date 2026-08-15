package com.example.nodekiosk

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File
import java.net.URI
import java.security.MessageDigest

object KioskConfig {
    const val DEFAULT_SERVER_URL = "http://192.168.1.5:8000/"
    const val DEFAULT_APP_NAME = "Kiosk"
    const val DEFAULT_ADMIN_PASSWORD_SHA256 = "5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5"
    private const val PREFS = "kiosk_settings"
    private const val URL_KEY = "server_url"
    private const val PASSWORD_KEY = "admin_password_hash"
    private const val APP_NAME_KEY = "app_name"
    private const val CUSTOM_ICON_KEY = "custom_icon_path"

    fun serverUrl(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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

    fun appName(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(APP_NAME_KEY, DEFAULT_APP_NAME) ?: DEFAULT_APP_NAME

    fun saveAppName(context: Context, name: String) {
        val finalName = name.trim().ifBlank { DEFAULT_APP_NAME }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(APP_NAME_KEY, finalName).apply()
    }

    fun customIconPath(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(CUSTOM_ICON_KEY, null)

    fun saveCustomIconFromUri(context: Context, uri: Uri): Boolean = try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val file = File(context.filesDir, "custom_kiosk_icon.png")
            file.outputStream().use { output -> inputStream.copyTo(output) }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(CUSTOM_ICON_KEY, file.absolutePath).apply()
            true
        } else false
    } catch (_: Exception) { false }

    fun loadAppIcon(context: Context): Drawable {
        val path = customIconPath(context)
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) return BitmapDrawable(context.resources, bitmap)
            }
        }
        return context.packageManager.getApplicationIcon(context.packageName)
    }

    fun validHttpUrl(value: String): Boolean = try {
        val uri = URI(value.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }
}
