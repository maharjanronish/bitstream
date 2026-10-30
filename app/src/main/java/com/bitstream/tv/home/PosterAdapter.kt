package com.bitstream.tv.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bitstream.tv.R
import com.bitstream.tv.core.Title
import com.bitstream.tv.core.Tmdb

/**
 * Poster card used by the home rows and the search grid.
 * D-pad focus -> scale-up + accent ring; OK -> play.
 */
class PosterAdapter(private val onClick: (Title) -> Unit) :
    ListAdapter<Title, PosterAdapter.VH>(Diff) {

    var nearEnd: (() -> Unit)? = null
    var showBadges: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poster, parent, false)
        return VH(view, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), showBadges)
        if (position >= itemCount - 4) nearEnd?.invoke()
    }

    class VH(view: View, private val onClick: (Title) -> Unit) : RecyclerView.ViewHolder(view) {
        private val card: View = view.findViewById(R.id.posterCard)
        private val image: ImageView = view.findViewById(R.id.posterImg)
        private val title: TextView = view.findViewById(R.id.posterTitle)
        private val sub: TextView = view.findViewById(R.id.posterSub)
        private val badge: TextView = view.findViewById(R.id.typeBadge)

        init {
            card.isFocusableInTouchMode = false
            card.setOnClickListener { (card.tag as? Title)?.let(onClick) }
            card.setOnFocusChangeListener { _, hasFocus ->
                val scale = if (hasFocus) 1.07f else 1f
                card.animate().scaleX(scale).scaleY(scale).setDuration(130).start()
            }
        }

        fun bind(item: Title, showBadge: Boolean) {
            card.tag = item
            title.text = item.displayName
            sub.text = item.year
            badge.visibility = if (showBadge) View.VISIBLE else View.GONE
            badge.text =
                if (item.isTv) itemView.context.getString(R.string.type_tv)
                else itemView.context.getString(R.string.type_movie)
            image.load(Tmdb.posterUrl(item.poster_path))
        }
    }

    object Diff : DiffUtil.ItemCallback<Title>() {
        override fun areItemsTheSame(a: Title, b: Title) = a.id == b.id && a.media_type == b.media_type
        override fun areContentsTheSame(a: Title, b: Title) = a == b
    }
}
