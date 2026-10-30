package com.bitstream.tv.provider

import java.io.IOException

/**
 * VidFast embed provider.
 * Movie: https://vidfast.vc/movie/{imdbId}?autoPlay=true
 * TV:    https://vidfast.vc/tv/{imdbId}/{season}/{episode}?autoPlay=true
 */
object VidFastProvider : EmbedProvider {

    override val id: String = "vidfast"
    override val name: String = "VidFast"

    override val navHosts: Set<String> = setOf("vidfast.vc", "vidfast.pro")

    private const val BASE = "https://vidfast.vc"

    override fun movieUrl(ids: TitleIds): String {
        val imdb = ids.imdbId ?: throw IOException("VidFast needs an IMDb id")
        return "$BASE/movie/$imdb?autoPlay=true"
    }

    override fun tvUrl(ids: TitleIds, season: Int, episode: Int): String {
        val imdb = ids.imdbId ?: throw IOException("VidFast needs an IMDb id")
        return "$BASE/tv/$imdb/$season/$episode?autoPlay=true"
    }
}
