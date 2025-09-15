package dev.kotlinbr

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @BeforeTest
    fun clearAppEnvPropertyBefore() {
        System.clearProperty("APP_ENV")
    }

    @AfterTest
    fun clearAppEnvPropertyAfter() {
        System.clearProperty("APP_ENV")
    }

    @Test
    fun testRoot() =
        testApplication {
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            client.get("/").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }

    @Test
    fun testHealth() =
        testApplication {
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            val response: HttpResponse = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            val body = response.bodyAsText().trim()
            assertEquals("""{"status":"ok"}""", body)
            // X-Request-ID must be present in response
            val respId = response.headers["X-Request-ID"]
            kotlin.test.assertTrue(!respId.isNullOrBlank(), "X-Request-ID header should be set")
        }

    @Test
    fun testXRequestIdPropagation() =
        testApplication {
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            val customId = "test-corr-id-123"
            val response: HttpResponse =
                client.get("/health") {
                    header("X-Request-ID", customId)
                }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(customId, response.headers["X-Request-ID"])
        }

    @Test
    fun testEnvLoadedFromConfigProperty() =
        testApplication {
            environment {
                config =
                    MapApplicationConfig(
                        "app.env" to "test",
                    )
            }
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            val response = client.get("/env")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            val body = response.bodyAsText().trim()
            assertEquals("""{"env":"test"}""", body)
        }

    @Test
    fun testEnvDefaultIsDevWhenNotProvided() =
        testApplication {
            application {
                module()
            }
            val response = client.get("/env")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText().trim()
            assertEquals("""{"env":"dev"}""", body)
        }

    @Test
    fun testEnvLoadedFromAppEnvSystemProperty() =
        testApplication {
            // Simulate APP_ENV environment via system property for tests
            System.setProperty("APP_ENV", "test")
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            val response = client.get("/env")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText().trim()
            assertEquals("""{"env":"test"}""", body)
        }
}
