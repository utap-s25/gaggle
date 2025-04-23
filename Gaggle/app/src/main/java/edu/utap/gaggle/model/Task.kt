package edu.utap.gaggle.model

import java.time.LocalDate

data class Task(
    var title: String = "",
    var date: LocalDate = LocalDate.now(),
    var completed: Boolean = false,
    var gaggleTitle: String = "", // for grouping in ProgressFragment
    var userId: String = "",
    var timestamp: Long = 0 // timestamp of completion
)