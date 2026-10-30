package com.bitstream.tv

import com.bitstream.tv.provider.Providers
import com.bitstream.tv.provider.TitleIds
import com.bitstream.tv.provider.VidApiProvider
import com.bitstream.tv.provider.VidFastProvider
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Validates embed URL construction for every registered provider. */
class ProvidersTest {

    private val ids = TitleIds(imdbId = "tt1375666", tmdbId = 27205L)

    @Test
    fun vidFastMovieUrl() {
        assertEquals(
            "https://vidfast.vc/movie/tt1375666?autoPlay=true",
            VidFastProvider.movieUrl(ids),
        )
    }

    @Test
    fun vidFastTvUrl() {
        assertEquals(
            "https://vidfast.vc/tv/tt1375666/2/5?autoPlay=true",
            VidFastProvider.tvUrl(ids, 2, 5),
        )
    }

    @Test
    fun vidFastNeedsImdb() {
        assertFailsWith<IOException> {
            VidFastProvider.movieUrl(TitleIds(tmdbId = 27205L))
        }
    }

    @Test
    fun vidApiMovieUrl() {
        assertEquals(
            "https://vaplayer.ru/embed/movie/tt1375666?autoplay=1",
            VidApiProvider.movieUrl(ids),
        )
    }

    @Test
    fun vidApiTvUrl() {
        assertEquals(
            "https://vaplayer.ru/embed/tv/tt1375666/1/1?autoplay=1",
            VidApiProvider.tvUrl(ids, 1, 1),
        )
    }

    @Test
    fun vidApiFallsBackToTmdbId() {
        // Titles without an IMDb id still play via VidAPI's TMDB support.
        assertEquals(
            "https://vaplayer.ru/embed/movie/27205?autoplay=1",
            VidApiProvider.movieUrl(TitleIds(tmdbId = 27205L)),
        )
    }

    @Test
    fun vidApiNeedsAnyId() {
        assertFailsWith<IOException> {
            VidApiProvider.movieUrl(TitleIds())
        }
    }

    @Test
    fun registryLooksUpAndLists() {
        assertEquals("vidfast", Providers.default.id)
        assertEquals(VidApiProvider, Providers.byId("vidapi"))
        assertEquals(null, Providers.byId("nope"))
        assertTrue(Providers.all().size >= 2)
        assertTrue(Providers.allNavHosts.containsAll(VidFastProvider.navHosts))
        assertTrue(Providers.allNavHosts.containsAll(VidApiProvider.navHosts))
    }
}
