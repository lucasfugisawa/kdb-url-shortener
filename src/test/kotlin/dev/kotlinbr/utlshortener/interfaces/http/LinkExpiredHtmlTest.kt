package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinkExpiredHtmlTest : BaseIntegrationTest() {
    @Test
    fun `GET slug should return 404 html when link is expired`() =
        testApplication {
            val config = initDatabaseInSchema("link_expired_html")
            System.setProperty("DB_URL", config.db.url)
            application {
                module()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                    followRedirects = false
                }

            // Create an expired link
            val shortenResponse =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ShortenRequest(
                            url = "https://kotlinlang.org",
                            expiresAt = OffsetDateTime.now().minusMinutes(1).toString(),
                        ),
                    )
                }
            val slug = shortenResponse.bodyAsText().substringAfter("\"slug\":\"").substringBefore("\"")

            // Try to access it via top-level route
            val response = client.get("/$slug")

            // Currently it returns Gone (410) and JSON.
            // The requirement is to return 404.html.
            // We expect it to return OK (200) with HTML content, or maybe 404 (NotFound) with HTML content.
            // Usually, 404.html is served with 404 status.

            assertEquals(HttpStatusCode.NotFound, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("<!DOCTYPE html>"), "Response should be HTML")
            assertTrue(body.contains("Page Not Found - 404"), "Response should be the 404 page")
        }
}
