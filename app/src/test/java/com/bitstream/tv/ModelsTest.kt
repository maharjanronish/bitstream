package com.bitstream.tv

import com.bitstream.tv.core.SeasonResponse
import com.bitstream.tv.core.Title
import com.bitstream.tv.core.TrendingResponse
import com.bitstream.tv.core.TvDetail
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Validates the @Serializable models against real-shaped TMDB responses. */
class ModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun parsesTrendingMovie() {
        val sample = """
            {"page":1,"results":[{"adult":false,"backdrop_path":"/cK.jpg","id":1283515,
            "title":"Verity","original_title":"Verity","overview":"A writer is hired…",
            "poster_path":"/cV.jpg","media_type":"movie","release_date":"2025-10-08",
            "vote_average":7.5,"vote_count":412}],"total_pages":12,"total_results":237}
        """.trimIndent()
        val parsed = json.decodeFromString<TrendingResponse>(sample)
        val item = parsed.results.first()
        assertEquals(1283515L, item.id)
        assertEquals("Verity", item.displayName)
        assertEquals("2025", item.year)
        assertFalse(item.isTv)
    }

    @Test
    fun parsesTvDetailWithExternalIds() {
        val sample = """
            {"name":"Game of Thrones","number_of_seasons":8,"first_air_date":"2011-04-17",
            "poster_path":"/x.jpg","overview":"…","external_ids":{"imdb_id":"tt0944947",
            "tvdb_id":0,"facebook":"got"},"genres":[]}
        """.trimIndent()
        val parsed = json.decodeFromString<TvDetail>(sample)
        assertEquals(8, parsed.number_of_seasons)
        assertEquals("tt0944947", parsed.external_ids?.imdb_id)
    }

    @Test
    fun parsesSeasonEpisodes() {
        val sample = """
            {"id":3624,"name":"Season 1","episodes":[{"id":63051,"name":"Winter Is Coming",
            "episode_number":1,"air_date":"2011-04-17","overview":"Eddard Stark…",
            "still_path":"/w.jpg","runtime":62},{"id":63052,"name":"The Kingsroad",
            "episode_number":2,"air_date":"2011-04-24","still_path":null}]}
        """.trimIndent()
        val parsed = json.decodeFromString<SeasonResponse>(sample)
        assertEquals(2, parsed.episodes.size)
        assertEquals(1, parsed.episodes.first().episode_number)
        assertEquals(null, parsed.episodes[1].still_path)
    }

    @Test
    fun titleFallsBackBetweenMovieAndTvFields() {
        val tv = json.decodeFromString<Title>(
            """{"id":1,"name":"Some Show","media_type":"tv","first_air_date":"2020-05-01"}"""
        )
        assertTrue(tv.isTv)
        assertEquals("Some Show", tv.displayName)
        assertEquals("2020", tv.year)

        val movie = json.decodeFromString<Title>(
            """{"id":2,"title":"Some Film","media_type":"movie","release_date":"1999-03-31"}"""
        )
        assertFalse(movie.isTv)
        assertEquals("Some Film", movie.displayName)
        assertEquals("1999", movie.year)
    }
}
