package dev.kotlinbr.utlshortener.infrastructure.repository

import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class LinksRepository {
    fun findAll(): List<Link> =
        transaction {
            LinksTable
                .selectAll()
                .map { it.toDomain() }
        }

    fun existsBySlug(slug: String): Boolean =
        transaction {
            LinksTable
                .selectAll() // SELECT *
                .andWhere { LinksTable.slug eq slug } // WHERE slug = <valor>
                .limit(1) // só uma linha
                .any() // retorna true se houver pelo menos uma linha
        }

    fun findBySlug(slug: String): Link? =
        transaction {
            LinksTable
                .selectAll() // SELECT *
                .andWhere { LinksTable.slug eq slug } // WHERE slug = <valor>
                .limit(1) // só uma linha
                .singleOrNull() // retorna linha ou null
                ?.toDomain() // converte para Link se não for null
        }

    fun save(link: Link): Link =
        transaction {
            val stmt =
                LinksTable.insert {
                    it[slug] = link.slug
                    it[targetUrl] = link.targetUrl
                    it[createdAt] = link.createdAt
                    it[isActive] = link.isActive
                    it[expiresAt] = link.expiresAt
                }
            val insertedRow: ResultRow =
                stmt.resultedValues?.singleOrNull()
                    ?: error("Failed to retrieve inserted row for link with slug='${link.slug}'")
            insertedRow.toDomain()
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
