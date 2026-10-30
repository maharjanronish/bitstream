package com.bitstream.tv.provider

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

    override fun movieUrl(imdbId: String): String = "$BASE/movie/$imdbId?autoPlay=true"

    override fun tvUrl(imdbId: String, season: Int, episode: Int): String =
        "$BASE/tv/$imdbId/$season/$episode?autoPlay=true"
}
