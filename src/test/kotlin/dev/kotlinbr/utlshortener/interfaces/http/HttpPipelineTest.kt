package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.MissingApplicationPluginException
import io.ktor.server.application.plugin
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpPipelineTest {
    @Test
    fun `compression plugin is installed in the pipeline`() =
        testApplication {
            // Ensure DB is skipped to keep test fast and isolated
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application {
                module()
                // Assert Compression plugin is installed on the Application
                try {
                    plugin(Compression)
                } catch (e: MissingApplicationPluginException) {
                    throw AssertionError("Compression plugin should be installed", e)
                }
            }

            // Also verify that a simple request succeeds when requesting gzip
            // (actual compression may be skipped for tiny payloads)
            val response =
                client.get("/") {
                    header(HttpHeaders.AcceptEncoding, "gzip")
                }
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `propagates X-Request-ID from request to response`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            val givenId = "abc-123"
            val response = client.get("/") { header("X-Request-ID", givenId) }

            assertEquals(HttpStatusCode.OK, response.status)
            val echoed = response.headers["X-Request-ID"]
            assertEquals(givenId, echoed)
        }

    @Test
    fun `generates X-Request-ID when absent and returns UUID-like value`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            val requestId = response.headers["X-Request-ID"]
            assertNotNull(requestId, "Response must include X-Request-ID")
            val uuidRegex = Regex("^[0-9a-fA-F-]{36}$")
            assertTrue(uuidRegex.matches(requestId), "X-Request-ID should look like a UUID: $requestId")
            // Also ensure body is still accessible
            assertEquals("Hello World!", response.bodyAsText())
        }
}
