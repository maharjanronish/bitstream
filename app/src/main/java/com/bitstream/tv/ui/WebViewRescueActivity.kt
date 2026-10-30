package com.bitstream.tv.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.bitstream.tv.R
import com.bitstream.tv.core.WebViewInfo

/**
 * One-time rescue screen for old boxes: their system WebView is too old for
 * the embed players (needs Chromium 80+). Guides the user to update it —
 * via Google Play when the box has it, otherwise by sideloading the last
 * compatible Google-signed WebView APK — then re-checks on demand.
 */
class WebViewRescueActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_rescue)

        renderStatus()

        findViewById<TextView>(R.id.rescueStep2).text =
            getString(
                R.string.webview_rescue_step2,
                WebViewInfo.variantFor(Build.SUPPORTED_ABIS.firstOrNull()),
            )

        findViewById<Button>(R.id.rescueOpen).apply {
            requestFocus()
            setOnClickListener { openUrl(getString(R.string.webview_download_url)) }
        }

        if (WebViewInfo.hasPlayStore(this)) {
            findViewById<Button>(R.id.rescuePlay).apply {
                visibility = View.VISIBLE
                setOnClickListener { openPlayStore() }
            }
        }

        findViewById<Button>(R.id.rescueCheck).setOnClickListener { checkAgain() }
    }

    private fun renderStatus() {
        val state = WebViewInfo.detect(this)
        findViewById<TextView>(R.id.rescueStatus).text =
            if (state.major > 0) {
                getString(R.string.webview_rescue_status, state.versionName, Build.VERSION.RELEASE)
            } else {
                getString(R.string.webview_rescue_status_unknown, Build.VERSION.RELEASE)
            }
    }

    private fun checkAgain() {
        val state = WebViewInfo.detect(this)
        val result = findViewById<TextView>(R.id.rescueResult)
        result.visibility = View.VISIBLE
        result.text = when {
            state.major >= WebViewInfo.MIN_MAJOR ->
                getString(R.string.webview_now_ok, state.versionName)

            state.isOld ->
                getString(R.string.still_old, state.major.toString())

            else -> getString(R.string.webview_not_detected)
        }
        renderStatus()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            // No browser on the box — the URL is printed on the screen above.
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore() {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.webview"))
            )
        } catch (e: Exception) {
            openUrl("https://play.google.com/store/apps/details?id=com.google.android.webview")
        }
    }
}
