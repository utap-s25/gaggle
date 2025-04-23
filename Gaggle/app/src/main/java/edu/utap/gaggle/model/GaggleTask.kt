package edu.utap.gaggle.model

import java.time.LocalDate

data class GaggleTask(
    var title: String,
    var date: LocalDate,
    var completed: Boolean = false,
    var gaggleTitle: String = "" // for grouping in ProgressFragment
)