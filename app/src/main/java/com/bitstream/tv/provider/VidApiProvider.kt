package com.bitstream.tv.provider

import java.io.IOException

/**
 * VidAPI embed provider (player host: vaplayer.ru).
 *
 * Docs: https://vidapi.ru/api — movies and TV both accept an IMDb id
 * (tt-prefixed) or a TMDB id (numeric). Autoplay: ?autoplay=1.
 *
 * Movie: https://vaplayer.ru/embed/movie/{imdbId}?autoplay=1
 * TV:    https://vaplayer.ru/embed/tv/{imdbId}/{season}/{episode}?autoplay=1
 */
object VidApiProvider : EmbedProvider {

    override val id: String = "vidapi"
    override val name: String = "VidAPI"

    override val navHosts: Set<String> = setOf("vaplayer.ru", "vidapi.ru")

    private const val BASE = "https://vaplayer.ru"

    /** IMDb when we have it, TMDB numeric otherwise. */
    private fun TitleIds.anyId(): String =
        imdbId ?: (tmdbId.takeIf { it > 0 }?.toString())
        ?: throw IOException("VidAPI needs an IMDb or TMDB id")

    override fun movieUrl(ids: TitleIds): String =
        "$BASE/embed/movie/${ids.anyId()}?autoplay=1"

    override fun tvUrl(ids: TitleIds, season: Int, episode: Int): String =
        "$BASE/embed/tv/${ids.anyId()}/$season/$episode?autoplay=1"
}
