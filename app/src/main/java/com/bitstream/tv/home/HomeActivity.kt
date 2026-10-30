package com.bitstream.tv.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitstream.tv.R
import com.bitstream.tv.core.Title
import com.bitstream.tv.search.SearchActivity
import com.bitstream.tv.ui.Routes
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()
    private lateinit var rowsAdapter: RowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<TextView>(R.id.searchBar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<Button>(R.id.homeRetry).setOnClickListener { vm.refresh() }

        val rows = findViewById<RecyclerView>(R.id.homeRows)
        rows.layoutManager = LinearLayoutManager(this)
        rowsAdapter = RowAdapter(
            onTitleClick = ::onTitleClicked,
            onNearEnd = { vm.loadMore(it) },
        )
        rows.adapter = rowsAdapter

        lifecycleScope.launch {
            vm.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect(::render)
        }
    }

    private fun render(state: HomeViewModel.UiState) {
        findViewById<View>(R.id.homeLoading).visibility =
            if (state.loading) View.VISIBLE else View.GONE
        findViewById<View>(R.id.homeError).visibility =
            if (state.error) View.VISIBLE else View.GONE
        findViewById<View>(R.id.homeContent).visibility =
            if (!state.loading && !state.error) View.VISIBLE else View.GONE
        if (!state.loading && !state.error) {
            rowsAdapter.submitList(listOfNotNull(state.movieRow, state.tvRow))
        }
    }

    private fun onTitleClicked(item: Title) {
        if (item.isTv) Routes.openShow(this, item.id, item.displayName, item.poster_path)
        else Routes.playMovie(this, item.id, item.displayName)
    }
}
