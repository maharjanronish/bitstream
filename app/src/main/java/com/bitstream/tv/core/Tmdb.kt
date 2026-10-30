package com.bitstream.tv.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

/**
 * Minimal TMDB client (read-only). One shared OkHttp client, no frameworks.
 */
object Tmdb {

    // Read-only TMDB API token (v4). If it gets abused, revoke/rotate at themoviedb.org.
    const val TOKEN =
        "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhYWQ5YmE5NGU2ZjZlZmNmODI2ZTlhMGIwZTM0NmIxZiIsIm5iZiI6MTczNzAxMzIwOS41ODA5OTk5LCJzdWIiOiI2Nzg4YjdkOWI5ZDFkMTBlZjE5NzdiNjMiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.xbdhQPtyCfwhFaOqpeTmYJaSfnjVl1RwGJvxqeAi2UE"

    private const val API = "https://api.themoviedb.org/3"
    const val IMAGE_BASE = "https://image.tmdb.org/t/p"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Volatile
    private var http: OkHttpClient? = null

    fun init(client: OkHttpClient) {
        http = client
    }

    private fun client(): OkHttpClient =
        requireNotNull(http) { "Tmdb.init() must be called from Application.onCreate()" }

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$API/$path")
            .header("Authorization", "Bearer $TOKEN")
            .build()
        client().newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("TMDB HTTP ${response.code}")
            body
        }
    }

    suspend fun trendingMovies(page: Int): List<Title> =
        json.decodeFromString<TrendingResponse>(get("trending/movie/week?page=$page"))
            .results.map { it.copy(media_type = "movie") }

    suspend fun trendingTv(page: Int): List<Title> =
        json.decodeFromString<TrendingResponse>(get("trending/tv/week?page=$page"))
            .results.map { it.copy(media_type = "tv") }

    suspend fun search(query: String, page: Int = 1): List<Title> =
        json.decodeFromString<SearchResponse>(
            get("search/multi?query=${encode(query)}&page=$page&include_adult=false")
        ).results.filter { it.media_type == "movie" || it.media_type == "tv" }

    suspend fun movieImdbId(tmdbId: Long): String? =
        json.decodeFromString<MovieDetail>(get("movie/$tmdbId")).imdb_id

    suspend fun tvShow(tmdbId: Long): TvDetail =
        json.decodeFromString<TvDetail>(get("tv/$tmdbId?append_to_response=external_ids"))

    suspend fun season(tmdbId: Long, season: Int): List<Episode> =
        json.decodeFromString<SeasonResponse>(get("tv/$tmdbId/season/$season")).episodes

    fun posterUrl(path: String?): String? = path?.let { "$IMAGE_BASE/w342$it" }

    fun stillUrl(path: String?): String? = path?.let { "$IMAGE_BASE/w300$it" }

    private fun encode(query: String): String = URLEncoder.encode(query, "UTF-8")
}
