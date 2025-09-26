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
)

fun Link.toResponse(): LinkResponse =
    LinkResponse(
        id = this.id ?: error("Link ID cannot be null"),
        slug = this.slug,
        targetUrl = this.targetUrl,
        createdAt = this.createdAt.toString(),
        isActive = this.isActive,
        expiresAt = this.expiresAt?.toString(),
    )

fun LinkResponse.toDomain(): Link =
    Link(
        id = this.id,
        slug = this.slug,
        targetUrl = this.targetUrl,
        createdAt = OffsetDateTime.parse(this.createdAt),
        isActive = this.isActive,
        expiresAt = this.expiresAt?.let { OffsetDateTime.parse(it) },
    )
