package dev.kotlinbr

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object HelloTable : Table("hello") {
    val id = long("id").autoIncrement()
    val message = text("message")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
