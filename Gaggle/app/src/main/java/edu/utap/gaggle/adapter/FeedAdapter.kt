package edu.utap.gaggle.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.utap.gaggle.R
import edu.utap.gaggle.model.FeedItem
import java.time.Duration
import java.time.LocalDateTime

class FeedAdapter : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(DiffCallback()) {

    class FeedViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val feedText: TextView = view.findViewById(R.id.feedText)
        private val feedSubText: TextView = view.findViewById(R.id.feedSubText)

        fun bind(feedItem: FeedItem) {
            Log.d("FeedAdapter", "Binding feed item: $feedItem")
            val displayText = "${feedItem.userName} completed '${feedItem.taskTitle}' 💪"
            val subText = "In ${feedItem.gaggleTitle} • ${timeAgo(feedItem.timestamp)}"

            feedText.text = displayText
            feedSubText.text = subText
        }

        private fun timeAgo(timestamp: Long?): String {
            val now = LocalDateTime.now()
            val dateTime = timestamp?.let {
                LocalDateTime.ofEpochSecond(it / 1000, 0, java.time.ZoneOffset.UTC)
            } ?: LocalDateTime.now()
            val duration = Duration.between(dateTime, now)

            return when {
                duration.toMinutes() < 1 -> "Just now"
                duration.toHours() < 1 -> "${duration.toMinutes()} minutes ago"
                duration.toDays() < 1 -> "${duration.toHours()} hours ago"
                duration.toDays() == 1L -> "Yesterday"
                else -> "${duration.toDays()} days ago"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.feed_item, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val feedItem = getItem(position)

        // Only bind the feedItem if it meets the criteria
        if (feedItem.timestamp != null && feedItem.completed != true) {
            holder.bind(feedItem)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.taskTitle == newItem.taskTitle
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem == newItem
        }
    }
}
