package edu.utap.gaggle.model

import java.time.LocalDate

data class FeedItem(
    val userName: String,
    val gaggleTitle: String,
    val taskTitle: String,
    val date: LocalDate,
    val timestamp: Long
)
