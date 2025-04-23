package edu.utap.gaggle.model

data class Gaggle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categories: List<String> = listOf(),
    val members: List<String> = listOf(),
    var tasks: List<String> = listOf()
)