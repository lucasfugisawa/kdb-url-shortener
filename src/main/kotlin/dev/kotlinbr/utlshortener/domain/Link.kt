package dev.kotlinbr.utlshortener.domain

import java.time.OffsetDateTime

data class Link(
    val id: Long? = null,
    val slug: String,
    val targetUrl: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val isActive: Boolean = true,
    val expiresAt: OffsetDateTime? = null,
)
