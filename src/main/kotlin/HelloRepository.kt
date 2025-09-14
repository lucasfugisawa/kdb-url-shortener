package dev.kotlinbr

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class HelloRepository {
    fun insert(message: String) = transaction {
        HelloTable.insert { stmt ->
            stmt[HelloTable.message] = message
        }
    }

    fun count(): Long = transaction {
        HelloTable.selectAll().count().toLong()
    }
}
