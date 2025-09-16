package dev.kotlinbr.utlshortener.testutils

import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID

object TestDataFactory {
    fun randomSlug(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(8)

    fun buildLink(
        id: Long = 0L,
        slug: String = randomSlug(),
        targetUrl: String = "https://example.com/${'$'}slug",
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        isActive: Boolean = true,
        expiresAt: OffsetDateTime? = null,
    ): Link =
        Link(
            id = id,
            slug = slug,
            targetUrl = targetUrl,
            createdAt = createdAt,
            isActive = isActive,
            expiresAt = expiresAt,
        )

    // --- SQL helpers (Exposed) ---

    fun insertLink(link: Link): Long =
        transaction {
            val id =
                LinksTable.insert {
                    it[slug] = link.slug
                    it[targetUrl] = link.targetUrl
                    it[createdAt] = link.createdAt
                    it[isActive] = link.isActive
                    it[expiresAt] = link.expiresAt
                } get LinksTable.id
            id
        }

    fun findLinkById(id: Long): Link? =
        transaction {
            LinksTable
                .selectAll()
                .where { LinksTable.id eq id }
                .limit(1)
                .firstOrNull()
                ?.toLink()
        }

    fun findLinkBySlug(slug: String): Link? =
        transaction {
            LinksTable
                .selectAll()
                .where { LinksTable.slug eq slug }
                .limit(1)
                .firstOrNull()
                ?.toLink()
        }

    private fun ResultRow.toLink(): Link =
        Link(
            id = this[LinksTable.id],
            slug = this[LinksTable.slug],
            targetUrl = this[LinksTable.targetUrl],
            createdAt = this[LinksTable.createdAt],
            isActive = this[LinksTable.isActive],
            expiresAt = this[LinksTable.expiresAt],
        )
}
