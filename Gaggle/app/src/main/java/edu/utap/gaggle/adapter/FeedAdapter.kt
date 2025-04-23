package edu.utap.gaggle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.model.FeedItem

class FeedAdapter : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(DiffCallback()) {

    class FeedViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(feedItem: FeedItem) {
            view.findViewById<TextView>(R.id.feedText).text =
                "${feedItem.userName} completed '${feedItem.taskTitle}' 💪"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem) = oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem) = oldItem == newItem
    }
}
