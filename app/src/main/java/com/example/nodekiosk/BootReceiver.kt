package com.example.nodekiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Boot received; requesting kiosk launch")
        val launch = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        runCatching { context.startActivity(launch) }.onFailure { Log.e(TAG, "Unable to launch after boot", it) }
    }
    companion object { private const val TAG = "NodeKioskBoot" }
}
