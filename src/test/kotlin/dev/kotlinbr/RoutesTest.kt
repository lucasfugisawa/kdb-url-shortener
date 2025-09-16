package dev.kotlinbr

import dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http.dto.LinkResponse
import dev.kotlinbr.utlshortener.testutils.TestClockUtils
import dev.kotlinbr.utlshortener.testutils.TestDataFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesUnitTest {
    @AfterEach
    fun clearToggles() {
        System.clearProperty("APP_SKIP_DB")
        System.clearProperty("APP_RUN_MIGRATIONS")
        System.clearProperty("APP_ENV")
        System.clearProperty("DB_URL")
        System.clearProperty("DB_USER")
        System.clearProperty("DB_PASSWORD")
    }

    @Test
    fun `5_1 frontend GET root`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            val res = client.get("/")
            assertEquals(HttpStatusCode.OK, res.status)
            val ct = res.headers[HttpHeaders.ContentType] ?: ""
            assertContains(ct.lowercase(), "text/plain")
            assertEquals("Hello World!", res.bodyAsText())
        }

    @Test
    fun `5_2 infra GET health`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/health")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("{\"status\":\"ok\"}", res.bodyAsText())
        }

    @Test
    fun `5_3 infra GET readiness not healthy when DB skipped`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/health/ready")
            assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
            assertEquals("{\"status\":\"not-ready\"}", res.bodyAsText())
        }

    @Test
    fun `5_5 infra GET env returns current env`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            System.setProperty("APP_ENV", "test")
            application { module() }
            val res = client.get("/env")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("{\"env\":\"test\"}", res.bodyAsText())
        }

    @Test
    fun `5_8 unknown route returns JSON 404`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/no-such-route")
            assertEquals(HttpStatusCode.NotFound, res.status)
            assertEquals("{\"code\":\"not_found\",\"message\":\"Resource not found\"}", res.bodyAsText())
        }

    @Test
    fun `5_9 internal error handler returns JSON`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application {
                module()
                routing {
                    get("/throw") { throw IllegalStateException("boom") }
                }
            }
            val res = client.get("/throw")
            assertEquals(HttpStatusCode.InternalServerError, res.status)
            assertEquals("{\"code\":\"internal_error\",\"message\":\"boom\"}", res.bodyAsText())
        }

    @Test
    fun `5_10 bad request handler returns JSON`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application {
                module()
                routing {
                    get("/bad") { call.respond(HttpStatusCode.BadRequest) }
                }
            }
            val res = client.get("/bad")
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("{\"code\":\"bad_request\",\"message\":\"Invalid request\"}", res.bodyAsText())
        }
}

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
    fun `5_4 readiness healthy when DB initialized`() =
        testApplication {
            setDbProps()
            application { module() }
            val res = client.get("/health/ready")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("{\"status\":\"ready\"}", res.bodyAsText())
        }

    @Test
    fun `5_6 api GET links returns empty array on empty DB`() =
        testApplication {
            setDbProps()
            application { module() }
            val res = client.get("/api/v1/links")
            assertEquals(HttpStatusCode.OK, res.status)
            val ct = res.headers[HttpHeaders.ContentType] ?: ""
            assertTrue(ct.lowercase().contains("application/json"))
            assertEquals("[]", res.bodyAsText())
        }

    @Test
    fun `5_7 api GET links returns data with ISO-8601 dates`() =
        testApplication {
            setDbProps()
            var id1: Long = 0
            var id2: Long = 0
            lateinit var link1: dev.kotlinbr.dev.kotlinbr.utlshortener.domain.Link
            lateinit var link2: dev.kotlinbr.dev.kotlinbr.utlshortener.domain.Link
            application {
                module()
                // Seed data after migrations
                val now = TestClockUtils.now()
                link1 = TestDataFactory.buildLink(id = 0, createdAt = now, isActive = true, expiresAt = null)
                link2 =
                    TestDataFactory.buildLink(id = 0, createdAt = now, isActive = false, expiresAt = now.plusDays(1))
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
            val r1 = byId[id1]!!
            val r2 = byId[id2]!!
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
}
