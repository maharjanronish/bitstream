package com.bitstream.tv.provider

import java.io.IOException

/**
 * The IDs of the title a provider should play. Every provider receives both
 * (resolved before the player opens), so each service can use whichever it
 * needs: some embeds key off IMDb (`tt…`), others off TMDB (numeric).
 */
data class TitleIds(
    val imdbId: String? = null,
    val tmdbId: Long = -1L,
)

/**
 * A video embed source. The rest of the app talks to providers only through
 * this interface, and the on-play "Choose source" overlay lists whatever is
 * registered in [Providers]. Adding a new embed service is therefore two
 * steps, with zero UI changes:
 *
 *  1. Create one small file next to the other providers:
 *
 *     object MyProvider : EmbedProvider {
 *         override val id = "myprovider"
 *         override val name = "My Provider"
 *         override val navHosts = setOf("myprovider.example")
 *         override fun movieUrl(ids: TitleIds) =
 *             "https://myprovider.example/embed/${ids.imdbId ?: error("missing imdb")}?autoplay=1"
 *         override fun tvUrl(ids: TitleIds, season: Int, episode: Int) =
 *             "https://myprovider.example/embed/${ids.imdbId}/$season/$episode?autoplay=1"
 *     }
 *
 *  2. Add it to the [Providers] registry list (one line). It now shows up in
 *     the chooser automatically.
 */
interface EmbedProvider {
    /** Stable machine id, e.g. used as the intent extra and prefs key. */
    val id: String

    /** Human name shown in the "Choose source" overlay. */
    val name: String

    /** Hosts the player WebView is allowed to navigate in the top frame. */
    val navHosts: Set<String>

    /** Build the embed URL for a movie. Throw [IOException] when the ids this
     *  service needs are missing. */
    fun movieUrl(ids: TitleIds): String

    /** Build the embed URL for a TV episode. Throw [IOException] when the ids
     *  this service needs are missing. */
    fun tvUrl(ids: TitleIds, season: Int, episode: Int): String
}

/** Registry of embed providers. The first registered one is the default. */
object Providers {
    private val registry = mutableListOf<EmbedProvider>(VidFastProvider, VidApiProvider)

    val default: EmbedProvider get() = registry.first()

    fun byId(id: String?): EmbedProvider? = registry.firstOrNull { it.id == id }

    /** Hosts allowed in the WebView top frame across all providers. */
    val allNavHosts: Set<String>
        get() = registry.flatMap { it.navHosts }.toSet()

    fun register(provider: EmbedProvider) {
        registry += provider
    }

    fun all(): List<EmbedProvider> = registry.toList()
}
