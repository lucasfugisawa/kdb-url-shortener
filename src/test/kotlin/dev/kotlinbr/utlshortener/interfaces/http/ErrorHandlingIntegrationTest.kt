package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ErrorHandlingIntegrationTest : BaseIntegrationTest() {
    @Test
    fun `should return URL_INVALID when url is malformed`() =
        testApplication {
            val appConfig = initDatabaseInSchema("error_url_invalid")
            application {
                attributes.put(dev.kotlinbr.utlshortener.app.config.AppConfigKey, appConfig)
                configureSerialization()
                configureRouting()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

            val response =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("url" to "invalid-url"))
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("URL_INVALID", error.code)
        }

    @Test
    fun `should return SLUG_NOT_FOUND when slug does not exist`() =
        testApplication {
            val appConfig = initDatabaseInSchema("error_slug_not_found")
            application {
                attributes.put(dev.kotlinbr.utlshortener.app.config.AppConfigKey, appConfig)
                configureSerialization()
                configureRouting()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

            val response = client.get("/api/v1/non-existent-slug")

            assertEquals(HttpStatusCode.NotFound, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("SLUG_NOT_FOUND", error.code)
        }

    @Test
    fun `should return VALIDATION_ERROR for invalid json`() =
        testApplication {
            val appConfig = initDatabaseInSchema("error_validation")
            application {
                attributes.put(dev.kotlinbr.utlshortener.app.config.AppConfigKey, appConfig)
                configureSerialization()
                configureRouting()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

            val response =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody("{ \"invalid\": \"json\" ") // Malformed JSON
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            val error = response.body<ErrorResponse>()
            assertEquals("VALIDATION_ERROR", error.code)
        }
}
