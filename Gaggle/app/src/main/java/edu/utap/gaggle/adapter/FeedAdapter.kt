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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FeedAdapter : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(DiffCallback()) {

    class FeedViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val feedText: TextView = view.findViewById(R.id.feedText)
        private val feedSubText: TextView = view.findViewById(R.id.feedSubText)

        fun bind(feedItem: FeedItem) {
            Log.d("FeedAdapter", "Binding feed item: $feedItem")
            val displayText = "${feedItem.userName} completed '${feedItem.taskTitle}' 💪"
            Log.d("FeedAdapter", "Timestamp: ${feedItem.timestamp}")
            val subText = "In ${feedItem.gaggleTitle} • ${timeAgo(feedItem.timestamp)}"

            feedText.text = displayText
            feedSubText.text = subText
        }

        private fun convertLongToDatetime(timestamp: Long?): LocalDateTime {
            if (timestamp == null) return LocalDateTime.now()
            val instant = Instant.ofEpochMilli(timestamp)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            return dateTime
        }

        private fun timeAgo(timestamp: Long?): String {
            if (timestamp == null) return "Unknown time"
            val now = LocalDateTime.now()
            val dateTime = convertLongToDatetime(timestamp)
            Log.d("FeedAdapter", "DateTime: $dateTime vs timestamp: $timestamp")
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

        if (feedItem.timestamp != null && feedItem.completed == true) {
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
