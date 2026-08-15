package com.example.nodekiosk

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AdminMenuActivity : android.app.Activity() {

    private lateinit var iconImageView: ImageView
    private lateinit var titleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))
        }

        iconImageView = ImageView(this).apply {
            setImageDrawable(KioskConfig.loadAppIcon(this@AdminMenuActivity))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }

        titleTextView = TextView(this).apply {
            text = KioskConfig.appName(this@AdminMenuActivity)
            textSize = 22f
            setPadding(dp(12), 0, 0, 0)
        }

        header.addView(iconImageView)
        header.addView(titleTextView)
        content.addView(header)

        content.addView(TextView(this).apply { text = "ADMIN MENU"; textSize = 14f; setTextColor(Color.GRAY) })
        content.addView(button("Return to Kiosk") { finish() })
        content.addView(button("Restart Kiosk App") { restartKiosk() })

        content.addView(TextView(this).apply {
            text = "Current server: ${KioskConfig.serverUrl(this@AdminMenuActivity)}"
            setPadding(0, dp(12), 0, dp(4))
        })

        content.addView(button("Set Server URL") { changeUrl() })
        content.addView(button("Set App Name") { changeAppName() })
        content.addView(button("Change Icon from Gallery") { pickIconFromGallery() })
        content.addView(button("Change Admin Password") { changePassword() })
        content.addView(button("Exit Kiosk") { confirmExit() })
        content.addView(button("Reboot Tablet") { confirmReboot() })
        content.addView(button("Shutdown Tablet (not supported)") { showShutdownLimitation() })

        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun restartKiosk() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun changeUrl() {
        val entry = EditText(this).apply {
            setText(KioskConfig.serverUrl(this@AdminMenuActivity))
            hint = "http://192.168.1.5:8000/"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this)
            .setTitle("Set kiosk server URL")
            .setMessage("This address is saved on the tablet and retried automatically when unavailable.")
            .setView(entry)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val url = entry.text.toString().trim()
                        if (KioskConfig.validHttpUrl(url)) {
                            KioskConfig.saveServerUrl(this, url)
                            dialog.dismiss()
                            restartKiosk()
                        } else entry.error = "Enter a valid http:// or https:// URL"
                    }
                }
                dialog.show()
            }
    }

    private fun changeAppName() {
        val entry = EditText(this).apply {
            setText(KioskConfig.appName(this@AdminMenuActivity))
            hint = "Gita GPT"
        }
        AlertDialog.Builder(this)
            .setTitle("Set App Name")
            .setMessage("Enter the display name for this kiosk application.")
            .setView(entry)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newName = entry.text.toString()
                KioskConfig.saveAppName(this, newName)
                titleTextView.text = KioskConfig.appName(this)
                Toast.makeText(this, "App name updated", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun pickIconFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        runCatching {
            startActivityForResult(Intent.createChooser(intent, "Select Kiosk Icon"), REQUEST_PICK_ICON)
        }.onFailure {
            Toast.makeText(this, "Gallery app not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_ICON && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                if (KioskConfig.saveCustomIconFromUri(this, uri)) {
                    iconImageView.setImageDrawable(KioskConfig.loadAppIcon(this))
                    Toast.makeText(this, "App icon updated from gallery!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun changePassword() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val pass1 = EditText(this).apply {
            hint = "New Admin Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val pass2 = EditText(this).apply {
            hint = "Confirm New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(pass1)
        layout.addView(pass2)
        AlertDialog.Builder(this)
            .setTitle("Change Admin Password")
            .setMessage("Enter a new password for accessing the Admin Menu.")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val p1 = pass1.text.toString()
                        val p2 = pass2.text.toString()
                        if (p1.isBlank()) pass1.error = "Password cannot be empty"
                        else if (p1 != p2) pass2.error = "Passwords do not match"
                        else {
                            KioskConfig.saveAdminPassword(this, p1)
                            Toast.makeText(this, "Admin password updated successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun confirmExit() = AlertDialog.Builder(this)
        .setTitle("Exit Kiosk?")
        .setMessage("This returns the tablet to normal Android for authorized maintenance.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Exit") { _, _ ->
            KioskDeviceManager.exitLockTask(this)
            finishAffinity()
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.show()

    private fun confirmReboot() = AlertDialog.Builder(this)
        .setTitle("Reboot Tablet?")
        .setMessage("The tablet will restart immediately.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Reboot") { _, _ ->
            if (!KioskDeviceManager.reboot(this)) Toast.makeText(this, "Reboot requires Device Owner support on this device.", Toast.LENGTH_LONG).show()
        }.show()

    private fun showShutdownLimitation() = AlertDialog.Builder(this)
        .setTitle("Shutdown unavailable")
        .setMessage("Android's public Device Owner API permits reboot, but does not provide a general silent power-off API. Use the device's authorized physical/management method.")
        .setPositiveButton("OK", null)
        .show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_PICK_ICON = 1001
    }
}
