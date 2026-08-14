package com.example.nodekiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

object KioskDeviceManager {
    private const val TAG = "NodeKioskPolicy"
    private fun admin(context: Context) = ComponentName(context, KioskDeviceAdminReceiver::class.java)
    fun isDeviceOwner(context: Context) = context.getSystemService(DevicePolicyManager::class.java).isDeviceOwnerApp(context.packageName)
    fun configureAndEnter(activity: Activity) {
        val dpm = activity.getSystemService(DevicePolicyManager::class.java)
        if (!isDeviceOwner(activity)) { Log.i(TAG, "Not Device Owner; Lock Task unavailable"); return }
        runCatching {
            dpm.setLockTaskPackages(admin(activity), arrayOf(activity.packageName))
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                // No Home, Overview, notifications, system-info, or global-actions affordances in kiosk mode.
                dpm.setLockTaskFeatures(admin(activity), DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME); addCategory(Intent.CATEGORY_DEFAULT) }
            dpm.addPersistentPreferredActivity(admin(activity), homeFilter, ComponentName(activity, MainActivity::class.java))
            if (android.os.Build.VERSION.SDK_INT >= 28) dpm.setStatusBarDisabled(admin(activity), true)
            activity.startLockTask()
            Log.i(TAG, "Lock Task entered")
        }.onFailure { Log.e(TAG, "Could not configure Lock Task", it) }
    }
    fun exitLockTask(activity: Activity) { runCatching { activity.stopLockTask() }.onFailure { Log.w(TAG, "Not in Lock Task", it) } }
    fun reboot(activity: Activity): Boolean = runCatching {
        if (!isDeviceOwner(activity)) return false
        activity.getSystemService(DevicePolicyManager::class.java).reboot(admin(activity)); true
    }.getOrElse { Log.e(TAG, "Reboot refused", it); false }
}
