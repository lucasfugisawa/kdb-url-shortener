package dev.kotlinbr

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object LinksTable : Table("links") {
    val id = long("id").autoIncrement()
    val slug = varchar("slug", 32).uniqueIndex()
    val targetUrl = text("target_url")
    val createdAt = timestampWithTimeZone("created_at")
    val isActive = bool("is_active").default(true)
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
