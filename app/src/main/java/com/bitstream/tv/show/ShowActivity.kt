package com.bitstream.tv.show

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bitstream.tv.R
import com.bitstream.tv.core.Tmdb
import com.bitstream.tv.ui.Routes
import com.bitstream.tv.ui.SourceChooser
import kotlinx.coroutines.launch

/** Season/episode picker for TV shows. Movies never come here — they play directly. */
class ShowActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TMDB_ID = "tmdb_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_POSTER = "poster_path"
    }

    private val vm: ShowViewModel by viewModels()
    private lateinit var episodeAdapter: EpisodeAdapter
    private var showTitle: String = ""
    private var showTmdbId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)

        showTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        showTmdbId = intent.getLongExtra(EXTRA_TMDB_ID, -1L)
        val tmdbId = showTmdbId

        findViewById<ImageView>(R.id.showPoster)
            .load(Tmdb.posterUrl(intent.getStringExtra(EXTRA_POSTER)))
        findViewById<TextView>(R.id.showTitle).text = showTitle

        episodeAdapter = EpisodeAdapter(
            onFocused = { episode -> renderEpisodeInfo(episode) },
            onClick = { episode -> playEpisode(episode) },
        )
        findViewById<RecyclerView>(R.id.epList).apply {
            layoutManager = LinearLayoutManager(this@ShowActivity)
            adapter = episodeAdapter
        }

        findViewById<TextView>(R.id.seasonPrev).setOnClickListener {
            vm.selectSeason(vm.state.value.season - 1)
        }
        findViewById<TextView>(R.id.seasonNext).setOnClickListener {
            vm.selectSeason(vm.state.value.season + 1)
        }
        findViewById<Button>(R.id.showRetry).setOnClickListener { vm.retry() }

        vm.load(tmdbId, showTitle)

        lifecycleScope.launch {
            vm.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect(::render)
        }
    }

    private fun render(state: ShowViewModel.UiState) {
        findViewById<View>(R.id.showLoading).visibility =
            if (state.loading) View.VISIBLE else View.GONE
        findViewById<View>(R.id.showError).visibility =
            if (state.error) View.VISIBLE else View.GONE
        findViewById<View>(R.id.showContent).visibility =
            if (!state.loading && !state.error) View.VISIBLE else View.GONE

        val show = state.show ?: return
        val titleView = findViewById<TextView>(R.id.showTitle)
        if (!show.name.isNullOrBlank()) titleView.text = show.name

        val year = show.first_air_date?.take(4).orEmpty()
        findViewById<TextView>(R.id.showMeta).text =
            listOf(year, "${show.number_of_seasons} seasons").filter { it.isNotBlank() }
                .joinToString("  ·  ")

        findViewById<TextView>(R.id.seasonLabel).text =
            getString(R.string.season_of_format, state.season, show.number_of_seasons)
        findViewById<View>(R.id.seasonLoading).visibility =
            if (state.seasonLoading) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.seasonPrev).apply {
            alpha = if (state.season > 1) 1f else 0.35f
            isFocusable = state.season > 1
        }
        findViewById<TextView>(R.id.seasonNext).apply {
            alpha = if (state.season < show.number_of_seasons) 1f else 0.35f
            isFocusable = state.season < show.number_of_seasons
        }

        episodeAdapter.submitList(state.episodes)
    }

    private fun renderEpisodeInfo(episode: com.bitstream.tv.core.Episode) {
        findViewById<TextView>(R.id.epOverview).text =
            episode.overview?.takeIf { it.isNotBlank() }
                ?: getString(R.string.no_description)
    }

    private fun playEpisode(episode: com.bitstream.tv.core.Episode) {
        val state = vm.state.value
        val epLabel = "S${state.season}E${episode.episode_number}"
        // IMDb may be null on rare titles — providers that also take TMDB
        // ids (e.g. VidAPI) can still play those.
        SourceChooser.show(this, "$showTitle · $epLabel") { provider ->
            Routes.playEpisode(
                this,
                state.show?.external_ids?.imdb_id,
                showTmdbId,
                state.season,
                episode.episode_number,
                "$showTitle · $epLabel",
                provider.id,
            )
        }
    }
}
