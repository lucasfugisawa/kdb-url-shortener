package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

fun Application.configureTestRouting() {
    configureErrorHandling()
    configureSerialization()
    configureApiRoutes()
}

@Tag("integration")
class UrlValidationIntegrationTest : BaseIntegrationTest() {
    @BeforeEach
    fun setup() {
        initDatabaseInSchema("url_validation_test")
    }

    @Test
    fun `POST shorten should accept valid url`() =
        testApplication {
            val cfg = createAppConfigForSchema("url_validation_test")
            application {
                attributes.put(AppConfigKey, cfg)
                configureTestRouting()
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
                    setBody(ShortenRequest(url = "https://kotlinlang.org"))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "invalid-url",
            "http://localhost",
            "ftp://files.com",
            "   ",
            "http://no-tld",
        ],
    )
    fun `POST shorten should return 400 for invalid urls`(url: String) =
        testApplication {
            val cfg = createAppConfigForSchema("url_validation_test")
            application {
                attributes.put(AppConfigKey, cfg)
                configureTestRouting()
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
                    setBody(ShortenRequest(url = url))
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `POST shorten should accept localhost if configured`() =
        testApplication {
            val cfg = createAppConfigForSchema("url_validation_test", allowLocalhost = true)
            application {
                attributes.put(AppConfigKey, cfg)
                configureTestRouting()
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
                    setBody(ShortenRequest(url = "http://localhost"))
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }
}
