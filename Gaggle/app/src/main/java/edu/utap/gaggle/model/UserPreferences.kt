package edu.utap.gaggle.model

data class UserPreferences(
    val username: String = "",
    val wantsPhysical: Boolean = false,
    val wantsMental: Boolean = false,
    val wantsCreative: Boolean = false,
    val wantsSocial: Boolean = false
)