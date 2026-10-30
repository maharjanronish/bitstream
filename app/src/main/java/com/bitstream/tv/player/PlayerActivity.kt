package com.bitstream.tv.player

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bitstream.tv.R
import com.bitstream.tv.core.Tmdb
import com.bitstream.tv.provider.Providers
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Fullscreen embed player. Runs in the dedicated `:player` process so that
 * leaving playback kills the process and every byte of WebView memory is
 * returned to the system instantly.
 *
 * Remote: D-pad/arrows control the embedded web player, BACK exits.
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_TMDB_ID = "tmdb_id"
        const val EXTRA_IMDB_ID = "imdb_id"
        const val EXTRA_SEASON = "season"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"

        const val KIND_MOVIE = "movie"
        const val KIND_TV = "tv"

        /** VidFast needs a reasonably modern JS engine in the WebView. */
        private const val MIN_WEBVIEW_MAJOR = 80
        private var webviewChecked = false
    }

    private lateinit var webView: WebView
    private var currentUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        findViewById<TextView>(R.id.playerTitle).text =
            intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<Button>(R.id.playerRetry).setOnClickListener { resolveAndLoad() }

        webView = findViewById(R.id.webView)
        setupWebView()
        checkWebViewAge()
        resolveAndLoad()
    }

    // ---------------------------------------------------------------- player

    private fun resolveAndLoad() {
        showLoading()
        val kind = intent.getStringExtra(EXTRA_KIND)
        val provider = Providers.default
        lifecycleScope.launch {
            try {
                val url = if (kind == KIND_TV) {
                    val imdbId = intent.getStringExtra(EXTRA_IMDB_ID).orEmpty()
                    provider.tvUrl(
                        imdbId,
                        intent.getIntExtra(EXTRA_SEASON, 1),
                        intent.getIntExtra(EXTRA_EPISODE, 1),
                    )
                } else {
                    val tmdbId = intent.getLongExtra(EXTRA_TMDB_ID, -1L)
                    val imdbId = Tmdb.movieImdbId(tmdbId)
                        ?: throw IOException("No IMDb id for this title")
                    provider.movieUrl(imdbId)
                }
                currentUrl = url
                webView.loadUrl(url)
            } catch (t: Throwable) {
                showError()
            }
        }
    }

    private fun setupWebView() {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false // allow autoPlay=true
                setSupportMultipleWindows(false)          // kills window.open popups
                javaScriptCanOpenWindowsAutomatically = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                cacheMode = WebSettings.LOAD_DEFAULT
                setSupportZoom(false)
                displayZoomControls = false
            }
            isFocusable = true
            isFocusableInTouchMode = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    val uri = request.url
                    val scheme = uri.scheme
                    if (scheme != "http" && scheme != "https") return true // intent:, market:, mailto: …
                    val host = uri.host ?: return true
                    // Keep the top frame inside the provider; iframe video hosts
                    // are unaffected by this check.
                    return !Providers.default.navHosts.any {
                        host == it || host.endsWith(".$it")
                    }
                }

                // API 23 fallback: no isForMainFrame flag there, so stay permissive
                // (popups are already blocked via setSupportMultipleWindows(false)).
                @Suppress("DEPRECATION", "OverridingDeprecatedMember")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val scheme = Uri.parse(url).scheme
                    return scheme != "http" && scheme != "https"
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    showLoading()
                }

                override fun onPageFinished(view: WebView, url: String) {
                    hideLoading()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) showError()
                }
            }

            webChromeClient = object : WebChromeClient() {
                private var customView: View? = null

                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (customView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    customView = view
                    webView.visibility = View.GONE
                    (window.decorView as ViewGroup).addView(
                        view,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }

                override fun onHideCustomView() {
                    customView?.let { (window.decorView as ViewGroup).removeView(it) }
                    customView = null
                    webView.visibility = View.VISIBLE
                }
            }
        }
    }

    /** Warn once (per process) if the system WebView is too old for the embed player. */
    private fun checkWebViewAge() {
        if (webviewChecked) return
        webviewChecked = true
        try {
            var version: String? = null
            for (pkg in listOf("com.google.android.webview", "com.android.webview")) {
                try {
                    version = packageManager.getPackageInfo(pkg, 0).versionName
                    break
                } catch (ignored: Exception) {
                }
            }
            val major = version?.split('.')?.firstOrNull()?.toIntOrNull() ?: return
            if (major < MIN_WEBVIEW_MAJOR) {
                val warning = findViewById<TextView>(R.id.webviewWarning)
                warning.visibility = View.VISIBLE
                warning.postDelayed({ warning.visibility = View.GONE }, 6000)
            }
        } catch (ignored: Throwable) {
        }
    }

    // ------------------------------------------------------------------ ui

    private fun showLoading() {
        findViewById<View>(R.id.playerLoading).visibility = View.VISIBLE
        findViewById<View>(R.id.playerError).visibility = View.GONE
    }

    private fun hideLoading() {
        findViewById<View>(R.id.playerLoading).visibility = View.GONE
    }

    private fun showError() {
        findViewById<View>(R.id.playerLoading).visibility = View.GONE
        findViewById<View>(R.id.playerError).visibility = View.VISIBLE
    }

    // ------------------------------------------------------------ lifecycle

    override fun onDestroy() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
        // Hard-kill the :player process so all WebView memory is freed at once.
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
