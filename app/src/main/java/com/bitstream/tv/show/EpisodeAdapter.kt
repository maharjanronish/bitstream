package com.bitstream.tv.show

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
import com.bitstream.tv.core.Episode
import com.bitstream.tv.core.Tmdb
import java.util.Locale

class EpisodeAdapter(
    private val onFocused: (Episode) -> Unit,
    private val onClick: (Episode) -> Unit,
) : ListAdapter<Episode, EpisodeAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return VH(view, onFocused, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        view: View,
        onFocused: (Episode) -> Unit,
        onClick: (Episode) -> Unit,
    ) : RecyclerView.ViewHolder(view) {
        private val number: TextView = view.findViewById(R.id.epNum)
        private val name: TextView = view.findViewById(R.id.epName)
        private val date: TextView = view.findViewById(R.id.epDate)
        private val still: ImageView = view.findViewById(R.id.epStill)

        init {
            itemView.setOnClickListener { (itemView.tag as? Episode)?.let(onClick) }
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) (itemView.tag as? Episode)?.let(onFocused)
            }
        }

        fun bind(item: Episode) {
            itemView.tag = item
            number.text = String.format(Locale.US, "%02d", item.episode_number)
            name.text = item.name ?: "Episode ${item.episode_number}"
            date.text = item.air_date
                ?: itemView.context.getString(R.string.no_air_date)
            still.load(Tmdb.stillUrl(item.still_path))
        }
    }

    object Diff : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(a: Episode, b: Episode) = a.id == b.id
        override fun areContentsTheSame(a: Episode, b: Episode) = a == b
    }
}
