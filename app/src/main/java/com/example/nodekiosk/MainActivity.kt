package com.example.nodekiosk

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.security.MessageDigest

class MainActivity : android.app.Activity() {
    private lateinit var webView: WebView
    private lateinit var errorPanel: LinearLayout
    private val retryHandler = Handler(Looper.getMainLooper())
    private var taps = 0
    private var firstTap = 0L
    private val retry = Runnable { if (errorPanel.visibility == View.VISIBLE) loadKioskUrl() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); Log.i(TAG, "Kiosk startup")
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        createContent(); hideSystemUi(); configureWebView(); loadKioskUrl()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { Log.i(TAG, "Back gesture ignored in kiosk") }
            )
        }
    }
    override fun onResume() { super.onResume(); hideSystemUi(); KioskDeviceManager.configureAndEnter(this) }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi() }
    override fun onDestroy() { retryHandler.removeCallbacksAndMessages(null); webView.destroy(); super.onDestroy() }
    @Deprecated("Back navigation is intentionally disabled for kiosk operation")
    override fun onBackPressed() { Log.i(TAG, "Back navigation ignored in kiosk") }

    private fun createContent() {
        val root = FrameLayout(this); webView = WebView(this); root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        errorPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(32,32,32)); visibility = View.GONE
            addView(TextView(context).apply { text = "Unable to connect to kiosk server."; setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER })
            addView(Button(context).apply { text = "Retry"; setOnClickListener { loadKioskUrl() } }) }
        root.addView(errorPanel, FrameLayout.LayoutParams(-1, -1))
        val trigger = View(this).apply { setOnClickListener { registerCornerTap() }; isHapticFeedbackEnabled = false }
        root.addView(trigger, FrameLayout.LayoutParams(dp(72), dp(72), Gravity.BOTTOM or Gravity.END)); setContentView(root)
    }
    @SuppressLint("SetJavaScriptEnabled") private fun configureWebView() { webView.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = false; allowFileAccess = false; allowContentAccess = false; setSupportMultipleWindows(false) }
        webView.isLongClickable = false; webView.setOnLongClickListener { true }; webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean { val scheme = request.url.scheme; return scheme != "http" && scheme != "https" }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) showConnectionError() }
        }
    }
    private fun loadKioskUrl() { val url = KioskConfig.serverUrl(this); Log.i(TAG, "Loading kiosk server: $url"); errorPanel.visibility = View.GONE; retryHandler.removeCallbacks(retry); webView.loadUrl(url) }
    private fun showConnectionError() { Log.w(TAG, "Kiosk server unavailable"); errorPanel.visibility = View.VISIBLE; retryHandler.removeCallbacks(retry); retryHandler.postDelayed(retry, RETRY_MS) }
    private fun registerCornerTap() { val now = System.currentTimeMillis(); if (now - firstTap > TAP_WINDOW_MS) { taps = 0; firstTap = now }; taps++; if (taps == 5) { taps = 0; firstTap = 0; showPasswordDialog() } }
    private fun showPasswordDialog() { Log.i(TAG, "Admin menu trigger activated"); val field = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        AlertDialog.Builder(this).setTitle("Administrator access").setView(field).setNegativeButton("Cancel", null).setPositiveButton("Continue") { _, _ -> if (passwordMatches(field.text.toString())) startActivity(android.content.Intent(this, AdminMenuActivity::class.java)) }.show()
    }
    private fun passwordMatches(value: String): Boolean { val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }; return digest == ADMIN_PASSWORD_SHA256 }
    private fun hideSystemUi() {
        window.insetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            // Do not expose transient system bars from an edge swipe. Lock Task enforces this policy for a Device Owner.
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    companion object { private const val TAG = "NodeKiosk"; private const val RETRY_MS = 15_000L; private const val TAP_WINDOW_MS = 3_000L
        // SHA-256 only; replace this digest with one generated for the deployment password before distribution.
        private const val ADMIN_PASSWORD_SHA256 = "5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5"
    }
}
