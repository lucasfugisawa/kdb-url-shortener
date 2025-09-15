package dev.kotlinbr.interfaces.http.dto

import dev.kotlinbr.domain.Link
import kotlinx.serialization.Serializable

@Serializable
data class LinkResponse(
    val id: Long,
    val slug: String,
    val targetUrl: String,
    val createdAt: String,
    val isActive: Boolean,
    val expiresAt: String? = null,
)

// Conversion extensions defined in the DTO layer to avoid leaking interface concerns into the domain or repository layers.
fun Link.toResponse(): LinkResponse =
    LinkResponse(
        id = this.id,
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
        createdAt = java.time.OffsetDateTime.parse(this.createdAt),
        isActive = this.isActive,
        expiresAt = this.expiresAt?.let { java.time.OffsetDateTime.parse(it) },
    )
