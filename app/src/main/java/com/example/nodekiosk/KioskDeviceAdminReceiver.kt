package com.example.nodekiosk

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KioskDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) { Log.i(TAG, "Device admin enabled") }
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence = "Disabling kiosk administration allows normal device use."
    companion object { const val TAG = "NodeKioskAdmin" }
}
