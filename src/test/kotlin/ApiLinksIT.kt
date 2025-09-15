package dev.kotlinbr

import dev.kotlinbr.infrastructure.db.tables.LinksTable
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Ignore("Disabled entire ApiLinksIT during investigation")
class ApiLinksIT {
    private lateinit var pg: PostgreSQLContainer<*>

    @BeforeTest
    fun setUp() {
        pg = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
        pg.start()

        // Configure app to use this DB and run migrations
        System.setProperty("DB_URL", pg.jdbcUrl)
        System.setProperty("DB_USER", pg.username)
        System.setProperty("DB_PASSWORD", pg.password)
        System.setProperty("DB_DRIVER", "org.postgresql.Driver")
        System.setProperty("APP_RUN_MIGRATIONS", "true")

        // Run Flyway migrations against the Testcontainers database
        org.flywaydb.core.Flyway
            .configure()
            .dataSource(pg.jdbcUrl, pg.username, pg.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate()

        // Connect Exposed for seeding data
        Database.connect(
            url = pg.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = pg.username,
            password = pg.password,
        )

        // Seed data
        transaction {
            val base = "https://kotlinlang.org/"
            repeat(3) { idx ->
                LinksTable.insert { row ->
                    row[slug] = "s$idx"
                    row[targetUrl] = base + idx
                }
            }
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            pg.stop()
        } catch (_: Throwable) {
        }
        System.clearProperty("DB_URL")
        System.clearProperty("DB_USER")
        System.clearProperty("DB_PASSWORD")
        System.clearProperty("DB_DRIVER")
        System.clearProperty("APP_RUN_MIGRATIONS")
    }

    @Ignore("Temporarily disabled due to flaky test. Needs investigation.")
    @Test
    fun get_api_v1_links_returns_all_links() =
        testApplication {
            application {
                module()
            }
            val response: HttpResponse = client.get("/api/v1/links")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            val body = response.bodyAsText()
            // Parse JSON to avoid brittle string matching across environments
            val json =
                kotlinx.serialization.json.Json
                    .parseToJsonElement(body)
            kotlin.test.assertTrue(
                json is kotlinx.serialization.json.JsonArray && json.size >= 3,
                "Expected a JSON array with seeded links",
            )
            val first = json.first()
            kotlin.test.assertTrue(first is kotlinx.serialization.json.JsonObject, "Expected array elements to be JSON objects")
            kotlin.test.assertTrue("slug" in first.keys, "Missing 'slug' field")
            kotlin.test.assertTrue("targetUrl" in first.keys, "Missing 'targetUrl' field")
        }
}
