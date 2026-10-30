package com.bitstream.tv.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitstream.tv.R
import com.bitstream.tv.core.Title

/** Vertical list of horizontal poster rows (one per media type). */
class RowAdapter(
    private val onTitleClick: (Title) -> Unit,
    private val onNearEnd: (String) -> Unit,
) : ListAdapter<HomeViewModel.Row, RowAdapter.RowVH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return RowVH(view, onTitleClick, onNearEnd)
    }

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        holder.bind(getItem(position))
    }

    class RowVH(
        view: View,
        onClick: (Title) -> Unit,
        private val nearEnd: (String) -> Unit,
    ) : RecyclerView.ViewHolder(view) {
        private val header: TextView = view.findViewById(R.id.rowTitle)
        private val list: RecyclerView = view.findViewById(R.id.rowList)
        private val posterAdapter = PosterAdapter(onClick)

        init {
            list.layoutManager =
                LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
            list.adapter = posterAdapter
        }

        fun bind(row: HomeViewModel.Row) {
            header.setText(if (row.type == "movie") R.string.trending_movies else R.string.trending_tv)
            posterAdapter.nearEnd = { nearEnd(row.type) }
            posterAdapter.submitList(row.items)
        }
    }

    object Diff : DiffUtil.ItemCallback<HomeViewModel.Row>() {
        override fun areItemsTheSame(a: HomeViewModel.Row, b: HomeViewModel.Row) = a.type == b.type
        override fun areContentsTheSame(a: HomeViewModel.Row, b: HomeViewModel.Row) = a == b
    }
}
