package dev.kotlinbr.utlshortener.infrastructure.repository

import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime

class LinksRepository {
    fun findAll(
        page: Int = 1,
        size: Int = 25,
    ): Pair<List<Link>, Long> =
        transaction {
            val total = LinksTable.selectAll().count()
            val offset = ((page - 1) * size).toLong()
            val items =
                LinksTable
                    .selectAll()
                    .limit(size)
                    .offset(offset)
                    .map { it.toDomain() }
            items to total
        }

    fun findBySlug(slug: String): Link? =
        transaction {
            LinksTable
                .selectAll()
                .where { LinksTable.slug eq slug }
                .singleOrNull()
                ?.toDomain()
        }

    fun existsBySlug(slug: String): Boolean =
        transaction {
            LinksTable
                .selectAll()
                .where { LinksTable.slug eq slug }
                .limit(1)
                .any()
        }

    fun incrementClicks(slug: String): Int =
        transaction {
            LinksTable.update({ LinksTable.slug eq slug }) {
                it[clicksCount] = clicksCount + 1
            }
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
                    it[maxClicks] = link.maxClicks
                }
            val insertedRow: ResultRow =
                stmt.resultedValues?.singleOrNull()
                    ?: error("Failed to retrieve inserted row for link with slug='${link.slug}'")
            insertedRow.toDomain()
        }

    fun deactivateExpiredLinks(now: OffsetDateTime = OffsetDateTime.now()): Int =
        transaction {
            LinksTable.update({
                LinksTable.isActive eq true and (
                    (LinksTable.expiresAt less now) or
                        (LinksTable.maxClicks.isNotNull() and (LinksTable.clicksCount greaterEq LinksTable.maxClicks))
                )
            }) {
                it[isActive] = false
            }
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
        clicksCount = this[LinksTable.clicksCount],
        maxClicks = this[LinksTable.maxClicks],
    )
