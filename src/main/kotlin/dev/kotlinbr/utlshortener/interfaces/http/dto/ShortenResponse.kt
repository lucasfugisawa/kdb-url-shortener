package dev.kotlinbr.utlshortener.interfaces.http.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShortenResponse(
    val slug: String,
    val path: String,
)
