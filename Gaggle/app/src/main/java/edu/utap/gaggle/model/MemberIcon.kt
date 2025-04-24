package edu.utap.gaggle.model

data class MemberIcon(
    val userId: String,
    val username: String,
    val profileImageUrl: String? = null
)