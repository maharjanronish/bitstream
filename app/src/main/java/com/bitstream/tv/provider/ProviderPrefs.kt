package com.bitstream.tv.provider

import android.content.Context

/**
 * Remembers the last embed source the user picked so the chooser can
 * pre-focus it next time. One string — not history, not favorites.
 */
object ProviderPrefs {
    private const val FILE = "bitstream_prefs"
    private const val KEY = "last_source"

    fun lastId(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, null)

    fun remember(context: Context, id: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, id).apply()
    }
}
