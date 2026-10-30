package com.bitstream.tv.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * System WebView facts + pure helpers for the old-box rescue flow.
 *
 * Cheap Android 6/7 boxes ship an AOSP WebView from ~2015 (Chromium 44–56)
 * whose JS engine cannot parse the modern, obfuscated embed players — that's
 * why playback fails there. The fix is a one-time system WebView update:
 * the last Google-signed release supporting Android 6.0 is 105.0.5195.136
 * (Android 7.0 got up to 114.x). Install it via Google Play when present,
 * or sideload the APK on boxes without Play.
 */
object WebViewInfo {

    /** Embed players need at least this Chromium major to parse their JS. */
    const val MIN_MAJOR = 80

    data class State(
        val packageName: String,
        val versionName: String,
        val major: Int,
    ) {
        /** True when the WebView is definitely too old (unknown stays false). */
        val isOld: Boolean get() = major in 1 until MIN_MAJOR
    }

    /** Chromium major of a WebView versionName like "105.0.5195.136" (0 = unknown). */
    fun majorOf(versionName: String?): Int =
        versionName?.split('.')?.firstOrNull()?.toIntOrNull() ?: 0

    /** The APK variant a user should download for a box with this primary ABI. */
    fun variantFor(primaryAbi: String?): String = when {
        primaryAbi == null -> "arm-v7a"
        primaryAbi.startsWith("arm64") -> "arm64-v8a"
        primaryAbi.startsWith("x86_64") -> "x86_64"
        primaryAbi.startsWith("x86") -> "x86"
        else -> "arm-v7a"
    }

    /** Detects the installed system WebView (Google package preferred, else AOSP). */
    fun detect(context: Context): State {
        var name = "com.android.webview"
        var version: String? = null
        for (pkg in listOf("com.google.android.webview", "com.android.webview")) {
            try {
                version = context.packageManager.getPackageInfo(pkg, 0).versionName
                name = pkg
                break
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        return State(name, version.orEmpty(), majorOf(version))
    }

    /** True when the box has a real Google Play Store installed. */
    fun hasPlayStore(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo("com.android.vending", 0)
            true
        } catch (e: Exception) {
            false
        }
}
