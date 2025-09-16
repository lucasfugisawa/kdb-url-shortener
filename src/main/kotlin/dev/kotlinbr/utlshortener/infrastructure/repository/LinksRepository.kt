package dev.kotlinbr.utlshortener.infrastructure.repository

import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class LinksRepository {
    fun findAll(): List<Link> =
        transaction {
            LinksTable
                .selectAll()
                .map { it.toDomain() }
        }
}

private fun ResultRow.toDomain(): Link =
    Link(
        id = this[LinksTable.id],
        slug = this[LinksTable.slug],
        targetUrl = this[LinksTable.targetUrl],
        createdAt = this[LinksTable.createdAt],
        isActive = this[LinksTable.isActive],
        expiresAt = this[LinksTable.expiresAt],
    )
