package com.bitstream.tv.provider

/**
 * A video embed source. Implement this to add a new provider — the rest of the
 * app talks to providers only through this interface, so adding e.g. a second
 * embed service is a one-file change:
 *
 *     object MyProvider : EmbedProvider {
 *         override val id = "myprovider"
 *         override val name = "My Provider"
 *         override val navHosts = setOf("myprovider.example")
 *         override fun movieUrl(imdbId: String) = "https://myprovider.example/embed/$imdbId"
 *         override fun tvUrl(imdbId: String, season: Int, episode: Int) =
 *             "https://myprovider.example/embed/$imdbId/$season/$episode"
 *     }
 *     Providers.register(MyProvider)
 */
interface EmbedProvider {
    val id: String
    val name: String

    /** Hosts the player WebView is allowed to navigate in the top frame. */
    val navHosts: Set<String>

    fun movieUrl(imdbId: String): String

    fun tvUrl(imdbId: String, season: Int, episode: Int): String
}

/** Registry of embed providers. The first registered one is the default. */
object Providers {
    private val registry = mutableListOf<EmbedProvider>(VidFastProvider)

    val default: EmbedProvider get() = registry.first()

    fun register(provider: EmbedProvider) {
        registry += provider
    }

    fun all(): List<EmbedProvider> = registry.toList()
}
