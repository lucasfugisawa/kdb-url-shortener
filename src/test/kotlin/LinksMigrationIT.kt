package dev.kotlinbr

import dev.kotlinbr.infrastructure.db.tables.LinksTable
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinksMigrationIT {
    private lateinit var pg: PostgreSQLContainer<*>

    @BeforeTest
    fun setUp() {
        pg = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
        pg.start()

        // Run Flyway migrations against the Testcontainers database
        Flyway
            .configure()
            .dataSource(pg.jdbcUrl, pg.username, pg.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate()

        // Connect Exposed to the same DB for CRUD checks
        Database.connect(
            url = pg.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = pg.username,
            password = pg.password,
        )
    }

    @AfterTest
    fun tearDown() {
        try {
            pg.stop()
        } catch (_: Throwable) {
        }
    }

    @Test
    fun `links table exists with expected columns and unique index`() {
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { conn ->
            conn.createStatement().use { st ->
                // Verify columns
                st
                    .executeQuery(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_name='links'
                        ORDER BY column_name
                        """.trimIndent(),
                    ).use { rs ->
                        val cols = mutableListOf<String>()
                        while (rs.next()) cols += rs.getString(1)
                        val expected = setOf("id", "slug", "target_url", "created_at", "is_active", "expires_at")
                        assertEquals(expected, cols.toSet(), "links table should have expected columns")
                    }

                // Verify unique index by name and uniqueness
                st
                    .executeQuery(
                        """
                        SELECT indexname, indexdef
                        FROM pg_indexes
                        WHERE tablename='links' AND indexname='links_slug_uindex'
                        """.trimIndent(),
                    ).use { rs ->
                        assertTrue(rs.next(), "Expected unique index links_slug_uindex to exist")
                        val def = rs.getString("indexdef")
                        assertNotNull(def)
                        assertTrue(def.contains("UNIQUE", ignoreCase = true), "Index should be UNIQUE: $def")
                    }
            }
        }
    }

    @Test
    fun `exposed can perform basic CRUD on links`() {
        val slug = "abc123"
        val target = "https://kotlinlang.org/"

        // Insert
        transaction {
            org.jetbrains.exposed.sql.SchemaUtils
                .createMissingTablesAndColumns() // no-op for existing
            LinksTable.insert { row ->
                row[LinksTable.slug] = slug
                row[LinksTable.targetUrl] = target
                // created_at defaults to now() in DB
                // is_active defaults to true in DB
            }
        }

        // Read
        transaction {
            val rows = LinksTable.selectAll().toList()
            assertTrue(rows.isNotEmpty(), "Should have at least one row in links after insert")
            val found = rows.first { it[LinksTable.slug] == slug }
            assertEquals(target, found[LinksTable.targetUrl])
            assertEquals(true, found[LinksTable.isActive])
        }
    }
}
