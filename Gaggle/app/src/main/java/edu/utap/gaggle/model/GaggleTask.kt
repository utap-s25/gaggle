package edu.utap.gaggle.model

import java.time.LocalDate

data class GaggleTask(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    var completed: Boolean = false
)