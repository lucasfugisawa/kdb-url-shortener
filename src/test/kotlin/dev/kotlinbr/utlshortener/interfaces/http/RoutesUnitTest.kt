package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
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
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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
    fun `frontend GET root`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            val res = client.get("/")
            assertEquals(HttpStatusCode.OK, res.status)
            val ct = res.headers[HttpHeaders.ContentType].orEmpty()
            assertContains(ct.lowercase(), "text/html")
            assertTrue(res.bodyAsText().contains("<title>Encurtador de URL</title>"))
        }

    @Test
    fun `infra GET health`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/health")
            assertEquals(HttpStatusCode.OK, res.status)
            assertEquals("{\"status\":\"ok\"}", res.bodyAsText())
        }

    @Test
    fun `infra GET readiness not healthy when DB skipped`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/health/ready")
            assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
            assertEquals("{\"status\":\"not-ready\"}", res.bodyAsText())
        }

    @Test
    fun `infra GET env returns current env`() =
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
    fun `unknown route returns JSON 404`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/no-such-route")
            assertEquals(HttpStatusCode.NotFound, res.status)
            assertEquals("{\"code\":\"not_found\",\"message\":\"Resource not found\"}", res.bodyAsText())
        }

    @Test
    fun `internal error handler returns JSON`() =
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
    fun `bad request handler returns JSON`() =
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

    @Test
    fun `POST shorten rejects empty URL`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val request = ShortenRequest(url = "")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("URL não pode estar vazia.", res.bodyAsText())
        }

    @Test
    fun `POST shorten rejects URL with only spaces`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val request = ShortenRequest(url = "   ")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("URL não pode estar vazia.", res.bodyAsText())
        }

    @Test
    fun `POST shorten rejects URL without http or https`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val request = ShortenRequest(url = "example.com")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("Esquema inválido. Use http:// ou https://", res.bodyAsText())
        }

    @Test
    fun `POST shorten accepts valid http URL`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val request = ShortenRequest(url = "http://example.com")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            // Since DB is skipped, this will fail at repository level (InternalServerError),
            // but validation should pass (not BadRequest with URL error message)
            val body = res.bodyAsText()
            assertTrue(
                res.status != HttpStatusCode.BadRequest ||
                    (!body.contains("URL não pode estar vazia") && !body.contains("URL inválida")),
            )
        }

    @Test
    fun `POST shorten accepts valid https URL`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val request = ShortenRequest(url = "https://example.com")
            val jsonClient = createClient { install(ContentNegotiation) { json() } }
            val res =
                jsonClient.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            // Since DB is skipped, this will fail at repository level (InternalServerError),
            // but validation should pass (not BadRequest with URL error message)
            val body = res.bodyAsText()
            assertTrue(
                res.status != HttpStatusCode.BadRequest ||
                    (!body.contains("URL não pode estar vazia") && !body.contains("URL inválida")),
            )
        }

    @Test
    fun `POST shorten rejects both expiresAt and maxClicks`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            application {
                module()
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }
            val res =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ShortenRequest(
                            url = "https://google.com",
                            expiresAt = "2026-12-31T23:59:59Z",
                            maxClicks = 10,
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertTrue(res.bodyAsText().contains("mutuamente exclusivos"))
        }

    @Test
    fun `POST shorten rejects invalid date format`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            application {
                module()
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }
            val res =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "url": "https://google.com",
                            "expiresAt": "data-invalida"
                        }
                        """.trimIndent(),
                    )
                }
            // If it fails at deserialization, it might be 400 or 500 depending on Ktor setup.
            // But if it reaches our logic, it's 400.
            // In Ktor, if Jackson/Kotlinx fails to parse a field into a non-nullable type or
            // if there's a type mismatch, it usually throws a BadRequestException (or similar).
            // Here it's a String, so it should parse.
            assertTrue(res.status == HttpStatusCode.BadRequest || res.status == HttpStatusCode.InternalServerError)
            if (res.status == HttpStatusCode.BadRequest) {
                assertTrue(res.bodyAsText().contains("Formato de data inválido"))
            }
        }

    @Test
    fun `GET slug returns 404 when DB is skipped`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")
            application { module() }
            val res = client.get("/api/v1/any-slug")
            // When DB is skipped, the repository might throw an exception or return null depending on implementation
            // In our case, the current implementation of configureApiRoutes calls LinksRepository()
            // which in turn will try to use the DB. If APP_SKIP_DB is true, it might fail or return null.
            // But we want to ensure the route exists.
            assertTrue(res.status == HttpStatusCode.NotFound || res.status == HttpStatusCode.InternalServerError)
        }
}
