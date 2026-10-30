package com.bitstream.tv.ui

import android.content.Context
import android.content.Intent
import com.bitstream.tv.player.PlayerActivity
import com.bitstream.tv.show.ShowActivity

/** Central navigation. */
object Routes {

    fun openShow(context: Context, tmdbId: Long, title: String, posterPath: String?) {
        context.startActivity(
            Intent(context, ShowActivity::class.java)
                .putExtra(ShowActivity.EXTRA_TMDB_ID, tmdbId)
                .putExtra(ShowActivity.EXTRA_TITLE, title)
                .putExtra(ShowActivity.EXTRA_POSTER, posterPath)
        )
    }

    fun playMovie(context: Context, tmdbId: Long, title: String, providerId: String) {
        context.startActivity(
            Intent(context, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_KIND, PlayerActivity.KIND_MOVIE)
                .putExtra(PlayerActivity.EXTRA_TMDB_ID, tmdbId)
                .putExtra(PlayerActivity.EXTRA_TITLE, title)
                .putExtra(PlayerActivity.EXTRA_PROVIDER_ID, providerId)
        )
    }

    fun playEpisode(
        context: Context,
        imdbId: String?,
        tmdbId: Long,
        season: Int,
        episode: Int,
        title: String,
        providerId: String,
    ) {
        context.startActivity(
            Intent(context, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_KIND, PlayerActivity.KIND_TV)
                .putExtra(PlayerActivity.EXTRA_IMDB_ID, imdbId)
                .putExtra(PlayerActivity.EXTRA_TMDB_ID, tmdbId)
                .putExtra(PlayerActivity.EXTRA_SEASON, season)
                .putExtra(PlayerActivity.EXTRA_EPISODE, episode)
                .putExtra(PlayerActivity.EXTRA_TITLE, title)
                .putExtra(PlayerActivity.EXTRA_PROVIDER_ID, providerId)
        )
    }
}
