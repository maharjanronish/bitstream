package com.bitstream.tv.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bitstream.tv.R
import com.bitstream.tv.core.Title
import com.bitstream.tv.home.PosterAdapter
import com.bitstream.tv.ui.Routes
import com.bitstream.tv.ui.SourceChooser
import kotlinx.coroutines.launch

class SearchActivity : ComponentActivity() {

    private val vm: SearchViewModel by viewModels()
    private lateinit var gridAdapter: PosterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val input = findViewById<EditText>(R.id.searchInput)
        val imm = getSystemService(InputMethodManager::class.java)

        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = vm.onQueryChanged(s?.toString().orEmpty())
        })
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                vm.searchNow()
                true
            } else {
                false
            }
        }
        // Hide the IME as soon as the user leaves the field to browse results.
        input.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
        input.requestFocus()
        input.postDelayed({ imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }, 200)

        findViewById<Button>(R.id.searchRetry).setOnClickListener { vm.retry() }

        val grid = findViewById<RecyclerView>(R.id.searchGrid)
        gridAdapter = PosterAdapter(::onTitleClicked)
        grid.adapter = gridAdapter
        grid.layoutManager = GridLayoutManager(this, columns())

        lifecycleScope.launch {
            vm.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect(::render)
        }
    }

    private fun columns(): Int =
        (resources.configuration.screenWidthDp / 132).coerceIn(3, 8)

    private fun render(state: SearchViewModel.UiState) {
        gridAdapter.showBadges = true
        gridAdapter.submitList(state.results)

        findViewById<View>(R.id.searchLoading).visibility =
            if (state.searching) View.VISIBLE else View.GONE
        findViewById<View>(R.id.searchError).visibility =
            if (state.error) View.VISIBLE else View.GONE

        val empty = findViewById<TextView>(R.id.searchEmpty)
        val showEmpty = !state.searching && !state.error &&
            (state.results.isEmpty() || !state.searched)
        empty.visibility = if (showEmpty) View.VISIBLE else View.GONE
        empty.setText(if (state.searched) R.string.no_results else R.string.type_to_search)
    }

    private fun onTitleClicked(item: Title) {
        if (item.isTv) {
            Routes.openShow(this, item.id, item.displayName, item.poster_path)
        } else {
            SourceChooser.show(this, item.displayName) { provider ->
                Routes.playMovie(this, item.id, item.displayName, provider.id)
            }
        }
    }
}
