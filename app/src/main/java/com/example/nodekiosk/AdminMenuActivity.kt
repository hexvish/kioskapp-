package com.example.nodekiosk

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*

class AdminMenuActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(28), dp(28), dp(28)) }
        content.addView(TextView(this).apply { text = "ADMIN MENU"; textSize = 24f })
        content.addView(button("Return to Kiosk") { finish() })
        content.addView(button("Restart Kiosk App") { restartKiosk() })
        content.addView(TextView(this).apply { text = "Current server: ${KioskConfig.serverUrl(this@AdminMenuActivity)}"; setPadding(0, dp(12), 0, dp(4)) })
        content.addView(button("Set Server URL") { changeUrl() })
        content.addView(button("Change Admin Password") { changePassword() })
        content.addView(button("Exit Kiosk") { confirmExit() })
        content.addView(button("Reboot Tablet") { confirmReboot() })
        content.addView(button("Shutdown Tablet (not supported)") { showShutdownLimitation() })
        setContentView(ScrollView(this).apply { addView(content) })
    }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
    private fun restartKiosk() { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)); finish() }
    private fun changeUrl() { val entry = EditText(this).apply { setText(KioskConfig.serverUrl(this@AdminMenuActivity)); hint = "http://192.168.1.5:8000/"; inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI }
        AlertDialog.Builder(this).setTitle("Set kiosk server URL").setMessage("This address is saved on the tablet and retried automatically when unavailable.").setView(entry).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create().also { dialog ->
            dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { val url = entry.text.toString().trim(); if (KioskConfig.validHttpUrl(url)) { KioskConfig.saveServerUrl(this, url); dialog.dismiss(); restartKiosk() } else entry.error = "Enter a valid http:// or https:// URL" } }; dialog.show()
        }
    }
    private fun changePassword() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
        val pass1 = EditText(this).apply { hint = "New Admin Password"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val pass2 = EditText(this).apply { hint = "Confirm New Password"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        layout.addView(pass1); layout.addView(pass2)
        AlertDialog.Builder(this).setTitle("Change Admin Password").setMessage("Enter a new password for accessing the Admin Menu.").setView(layout).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create().also { dialog ->
            dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val p1 = pass1.text.toString(); val p2 = pass2.text.toString()
                if (p1.isBlank()) pass1.error = "Password cannot be empty"
                else if (p1 != p2) pass2.error = "Passwords do not match"
                else { KioskConfig.saveAdminPassword(this, p1); Toast.makeText(this, "Admin password updated successfully", Toast.LENGTH_SHORT).show(); dialog.dismiss() }
            } }; dialog.show()
        }
    }
    private fun confirmExit() = AlertDialog.Builder(this).setTitle("Exit Kiosk?").setMessage("This returns the tablet to normal Android for authorized maintenance.").setNegativeButton("Cancel", null).setPositiveButton("Exit") { _, _ ->
        KioskDeviceManager.exitLockTask(this); finishAffinity(); startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.show()
    private fun confirmReboot() = AlertDialog.Builder(this).setTitle("Reboot Tablet?").setMessage("The tablet will restart immediately.").setNegativeButton("Cancel", null).setPositiveButton("Reboot") { _, _ ->
        if (!KioskDeviceManager.reboot(this)) Toast.makeText(this, "Reboot requires Device Owner support on this device.", Toast.LENGTH_LONG).show()
    }.show()
    private fun showShutdownLimitation() = AlertDialog.Builder(this).setTitle("Shutdown unavailable").setMessage("Android's public Device Owner API permits reboot, but does not provide a general silent power-off API. Use the device's authorized physical/management method.").setPositiveButton("OK", null).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
