package edu.utap.gaggle.model

data class FeedItem(
    val userName: String? = null,
    val gaggleTitle: String? = null,
    val taskTitle: String? = null,
    val date: String? = null,  // nullable and non-LocalDate to handle deserialization
    val timestamp: Long? = null,
    val completed: Boolean? = false // aka disable checkbox ; nullable to handle deserialization
) {
}