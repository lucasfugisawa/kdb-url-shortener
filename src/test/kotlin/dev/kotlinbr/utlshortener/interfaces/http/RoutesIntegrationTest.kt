package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import dev.kotlinbr.utlshortener.interfaces.http.dto.LinkResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.testutils.TestClockUtils
import dev.kotlinbr.utlshortener.testutils.TestDataFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("integration")
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class RoutesIntegrationTest {
    companion object {
        @JvmStatic
        @Container
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer(
                "postgres:16-alpine",
            ).apply { withReuse(true) }
    }

    @AfterEach
    fun clearProps() {
        System.clearProperty("APP_SKIP_DB")
        System.clearProperty("APP_RUN_MIGRATIONS")
        System.clearProperty("APP_ENV")
        System.clearProperty("DB_URL")
        System.clearProperty("DB_USER")
        System.clearProperty("DB_PASSWORD")
    }

    private fun setDbProps() {
        if (!postgres.isRunning) postgres.start()
        System.setProperty("APP_ENV", "test")
        System.setProperty("APP_SKIP_DB", "false")
        System.setProperty("APP_RUN_MIGRATIONS", "true")
        System.setProperty("DB_URL", postgres.jdbcUrl)
        System.setProperty("DB_USER", postgres.username)
        System.setProperty("DB_PASSWORD", postgres.password)
    }

    @Test
    fun `readiness healthy when DB initialized`() =
        testApplication {
            setDbProps()
            application { module() }
            val res = client.get("/health/ready")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("{\"status\":\"ready\"}", res.bodyAsText())
        }

    @Test
    fun `api GET links returns empty array on empty DB`() =
        testApplication {
            setDbProps()
            application { module() }
            val res = client.get("/api/v1/links")
            assertEquals(HttpStatusCode.OK, res.status)
            val ct = res.headers[HttpHeaders.ContentType].orEmpty()
            assertTrue(ct.lowercase().contains("application/json"))
            assertEquals("[]", res.bodyAsText())
        }

    @Test
    fun `api GET links returns data with ISO-8601 dates`() =
        testApplication {
            setDbProps()
            var id1: Long = 0
            var id2: Long = 0
            lateinit var link1: Link
            lateinit var link2: Link
            application {
                module()
                // Ensure a clean state for this test
                transaction { LinksTable.deleteAll() }
                // Seed data after migrations
                val now = TestClockUtils.now()
                link1 = TestDataFactory.buildLink(createdAt = now, isActive = true, expiresAt = null)
                link2 =
                    TestDataFactory.buildLink(createdAt = now, isActive = false, expiresAt = now.plusDays(1))
                id1 = TestDataFactory.insertLink(link1)
                id2 = TestDataFactory.insertLink(link2)
            }
            val res = client.get("/api/v1/links")
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val list = json.decodeFromString<List<LinkResponse>>(body)
            assertEquals(2, list.size)
            // Since IDs are auto-increment, ensure IDs returned are the inserted ones
            val ids = list.map { it.id }.toSet()
            assertTrue(ids.containsAll(listOf(id1, id2)))
            val byId = list.associateBy { it.id }
            val r1 = requireNotNull(byId[id1]) { "Response for id=$id1 not found" }
            val r2 = requireNotNull(byId[id2]) { "Response for id=$id2 not found" }
            assertEquals(link1.slug, r1.slug)
            assertEquals(link1.targetUrl, r1.targetUrl)
            assertEquals(link1.createdAt.toString(), r1.createdAt)
            assertEquals(true, r1.isActive)
            assertEquals(null, r1.expiresAt)

            assertEquals(link2.slug, r2.slug)
            assertEquals(link2.targetUrl, r2.targetUrl)
            assertEquals(link2.createdAt.toString(), r2.createdAt)
            assertEquals(false, r2.isActive)
            assertEquals(link2.expiresAt!!.toString(), r2.expiresAt)
        }

    @Test
    fun `api POST shorten creates link and verifies in DB`() =
        testApplication {
            setDbProps()
            application { module() }
            val request = ShortenRequest(url = "https://example.com/test")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.OK, res.status)
            val body = res.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val response = json.decodeFromString<ShortenResponse>(body)
            assertTrue(response.slug.isNotEmpty())
            assertEquals("/${response.slug}", response.path)

            // Verify presence in DB
            val linkInDb = TestDataFactory.findLinkBySlug(response.slug)
            requireNotNull(linkInDb) { "Link not found in DB" }
            assertEquals("https://example.com/test", linkInDb.targetUrl)
            assertEquals(true, linkInDb.isActive)
        }
}
