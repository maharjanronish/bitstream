package com.bitstream.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.bitstream.tv.core.Net
import com.bitstream.tv.core.Tmdb

class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Runs in every process (home + :player) so the TMDB client is always ready.
        Tmdb.init(Net.httpClient(this))
    }

    /** Single shared image pipeline: capped disk cache, no oversized memory cache. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { Net.httpClient(this) } // same TLS fix + connection pool
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("images"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .crossfade(140)
            .build()
}
