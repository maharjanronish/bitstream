package com.bitstream.tv.core

import kotlinx.serialization.Serializable

@Serializable
data class Title(
    val id: Long = 0,
    val media_type: String? = null, // "movie" | "tv"
    val title: String? = null,      // movies
    val name: String? = null,       // tv
    val poster_path: String? = null,
    val release_date: String? = null,
    val first_air_date: String? = null,
) {
    val displayName: String get() = name ?: title ?: "—"
    val year: String get() = (first_air_date ?: release_date)?.take(4).orEmpty()
    val isTv: Boolean get() = media_type == "tv"
}

@Serializable
data class TrendingResponse(val page: Int = 1, val results: List<Title> = emptyList())

@Serializable
data class SearchResponse(val results: List<Title> = emptyList())

@Serializable
data class ExternalIds(val imdb_id: String? = null)

@Serializable
data class MovieDetail(val imdb_id: String? = null)

@Serializable
data class TvDetail(
    val name: String? = null,
    val number_of_seasons: Int = 1,
    val first_air_date: String? = null,
    val poster_path: String? = null,
    val external_ids: ExternalIds? = null,
)

@Serializable
data class Episode(
    val id: Long = 0,
    val name: String? = null,
    val episode_number: Int = 0,
    val air_date: String? = null,
    val overview: String? = null,
    val still_path: String? = null,
)

@Serializable
data class SeasonResponse(val episodes: List<Episode> = emptyList())
