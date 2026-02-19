package dev.kotlinbr.utlshortener.interfaces.http.dto

import dev.kotlinbr.utlshortener.domain.Link
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class LinkResponse(
    val id: Long,
    val slug: String,
    val targetUrl: String,
    val createdAt: String,
    val isActive: Boolean,
    val expiresAt: String? = null,
    val clicksCount: Int = 0,
    val maxClicks: Int? = null,
)

@Serializable
data class StatsResponse(
    val slug: String,
    val clicks: Int,
)

fun Link.toResponse(): LinkResponse =
    LinkResponse(
        id = id ?: error("Link ID cannot be null"),
        slug = slug,
        targetUrl = targetUrl,
        createdAt = createdAt.toString(),
        isActive = isActive,
        expiresAt = expiresAt?.toString(),
        clicksCount = clicksCount,
        maxClicks = maxClicks,
    )

fun LinkResponse.toDomain(): Link =
    Link(
        id = id,
        slug = slug,
        targetUrl = targetUrl,
        createdAt = OffsetDateTime.parse(createdAt),
        isActive = isActive,
        expiresAt = expiresAt?.let { OffsetDateTime.parse(it) },
        clicksCount = clicksCount,
        maxClicks = maxClicks,
    )
