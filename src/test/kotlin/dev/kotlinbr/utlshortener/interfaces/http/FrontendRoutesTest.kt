package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrontendRoutesTest {
    @AfterEach
    fun tearDown() {
        System.clearProperty("APP_SKIP_DB")
    }

    @Test
    fun `root path should serve index html`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            application {
                module()
            }
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<title>URL Shortener</title>"), "Body should contain the title")
            assertTrue(body.contains("id=\"urlInput\""), "Body should contain the url input")
        }

    @Test
    fun `GET invalid slug returns 404`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            application {
                module()
            }
            val res = client.get("/api/v1/non-existent-slug")
            assertEquals(HttpStatusCode.NotFound, res.status)
        }
}
