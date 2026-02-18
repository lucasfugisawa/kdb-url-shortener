package dev.kotlinbr.utlshortener.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShortenRequest(
    val url: String,
    val expiresAt: String? = null,
    val maxClicks: Int? = null,
)
