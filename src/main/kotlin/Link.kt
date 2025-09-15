package dev.kotlinbr

import java.time.OffsetDateTime

data class Link(
    val id: Long,
    val slug: String,
    val targetUrl: String,
    val createdAt: OffsetDateTime,
    val isActive: Boolean = true,
    val expiresAt: OffsetDateTime? = null,
)
