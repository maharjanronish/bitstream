package com.bitstream.tv

import com.bitstream.tv.core.WebViewInfo
import com.bitstream.tv.core.WebViewInfo.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Validates the old-box WebView detection helpers. */
class WebViewInfoTest {

    @Test
    fun majorsParseFromVersionNames() {
        assertEquals(105, WebViewInfo.majorOf("105.0.5195.136"))
        assertEquals(44, WebViewInfo.majorOf("44.0.2403.90"))
        assertEquals(0, WebViewInfo.majorOf(null))
        assertEquals(0, WebViewInfo.majorOf("beta-3"))
        assertEquals(0, WebViewInfo.majorOf(""))
    }

    @Test
    fun oldnessThresholdIsChromium80() {
        assertTrue(State("com.android.webview", "44.0", 44).isOld)   // Android 6 AOSP
        assertTrue(State("com.android.webview", "79.1", 79).isOld)   // just under
        assertFalse(State("com.google.android.webview", "80.0", 80).isOld)
        assertFalse(State("com.google.android.webview", "105.0.5195.136", 105).isOld)
        assertFalse(State("com.android.webview", "", 0).isOld)       // unknown stays quiet
    }

    @Test
    fun downloadVariantMatchesBoxAbi() {
        assertEquals("arm64-v8a", WebViewInfo.variantFor("arm64-v8a"))
        assertEquals("arm-v7a", WebViewInfo.variantFor("armeabi-v7a"))
        assertEquals("x86", WebViewInfo.variantFor("x86"))
        assertEquals("x86_64", WebViewInfo.variantFor("x86_64"))
        assertEquals("arm-v7a", WebViewInfo.variantFor(null))
    }
}
