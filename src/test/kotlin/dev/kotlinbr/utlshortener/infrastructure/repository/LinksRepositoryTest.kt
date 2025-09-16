package dev.kotlinbr.utlshortener.infrastructure.repository

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppFlags
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DbConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.ServerConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import dev.kotlinbr.utlshortener.testutils.TestClockUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFails

@Tag("integration")
class LinksRepositoryTest : BaseIntegrationTest() {
    private fun createSchema(schema: String) {
        DriverManager.getConnection(jdbcUrl(), username(), password()).use { conn ->
            conn.createStatement().use { st ->
                st.execute("CREATE SCHEMA IF NOT EXISTS \"$schema\"")
            }
        }
    }

    private fun initDbInSchema(schema: String) {
        createSchema(schema)
        val base = jdbcUrl()
        val sep = if (base.contains("?")) "&" else "?"
        val cfg =
            AppConfig(
                env = "test",
                server = ServerConfig(port = 0),
                db =
                    DbConfig(
                        driver = "org.postgresql.Driver",
                        url = "$base${sep}currentSchema=$schema",
                        user = username(),
                        password = password(),
                        poolMax = 5,
                    ),
                flags = AppFlags(skipDb = false, runMigrations = true),
            )
        DatabaseFactory.resetForTesting()
        DatabaseFactory.init(cfg)
    }

    @Test
    fun `findAll returns rows mapped correctly`() {
        val schema = "s_repo_findall"
        initDbInSchema(schema)

        val created1: OffsetDateTime = TestClockUtils.now()
        val created2: OffsetDateTime = TestClockUtils.now().plusDays(1)

        transaction {
            LinksTable.insert {
                it[slug] = "a1"
                it[targetUrl] = "https://a.example/1"
                it[createdAt] = created1
                it[isActive] = true
                it[expiresAt] = null
            }
            LinksTable.insert {
                it[slug] = "b2"
                it[targetUrl] = "https://b.example/2"
                it[createdAt] = created2
                it[isActive] = false
                it[expiresAt] = created2.plusDays(30)
            }
        }

        val repo = LinksRepository()
        val list: List<Link> = repo.findAll()
        assertEquals(2, list.size)

        val a = list.first { it.slug == "a1" }
        assertEquals("https://a.example/1", a.targetUrl)
        assertEquals(created1, a.createdAt)
        assertEquals(true, a.isActive)
        assertEquals(null, a.expiresAt)

        val b = list.first { it.slug == "b2" }
        assertEquals("https://b.example/2", b.targetUrl)
        assertEquals(created2, b.createdAt)
        assertEquals(false, b.isActive)
        assertEquals(created2.plusDays(30), b.expiresAt)
    }

    @Test
    fun `unique slug constraint enforced`() {
        val schema = "s_repo_unique"
        initDbInSchema(schema)

        // First insert should succeed
        transaction {
            LinksTable.insert {
                it[slug] = "dup"
                it[targetUrl] = "https://dup.example/1"
                it[createdAt] = TestClockUtils.now()
                it[isActive] = true
                it[expiresAt] = null
            }
        }

        // Second insert with same slug should fail at DB level
        assertFails("Duplicate slug should violate unique constraint") {
            transaction {
                LinksTable.insert {
                    it[slug] = "dup"
                    it[targetUrl] = "https://dup.example/2"
                    it[createdAt] = TestClockUtils.now()
                    it[isActive] = true
                    it[expiresAt] = null
                }
            }
        }
    }
}
