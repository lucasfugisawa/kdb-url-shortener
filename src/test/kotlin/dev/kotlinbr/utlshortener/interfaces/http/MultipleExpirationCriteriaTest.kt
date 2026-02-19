package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
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

class MultipleExpirationCriteriaTest : BaseIntegrationTest() {
    @Test
    fun `api POST shorten should allow both expiresAt and maxClicks`() =
        testApplication {
            val config = initDatabaseInSchema("multiple_criteria_success")
            application {
                attributes.put(AppConfigKey, config)
                configureSerialization()
                configureErrorHandling()
                configureApiRoutes()
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
                    setBody(
                        ShortenRequest(
                            url = "https://kotlinlang.org",
                            expiresAt = OffsetDateTime.now().plusDays(1).toString(),
                            maxClicks = 10,
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `api GET slug should fail if ANY criteria is met - time expired`() =
        testApplication {
            val config = initDatabaseInSchema("criteria_time_expired")
            application {
                attributes.put(AppConfigKey, config)
                configureSerialization()
                configureErrorHandling()
                configureApiRoutes()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                }

            // Create a link that is already expired but has clicks remaining
            val shortenResponse =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ShortenRequest(
                            url = "https://kotlinlang.org",
                            expiresAt = OffsetDateTime.now().minusMinutes(1).toString(),
                            maxClicks = 10,
                        ),
                    )
                }
            val slug =
                shortenResponse.bodyAsText().let {
                    // Simple way to get slug from {"slug":"...", "shortUrl":"..."}
                    it.substringAfter("\"slug\":\"").substringBefore("\"")
                }

            val response = client.get("/api/v1/$slug")
            assertEquals(HttpStatusCode.Gone, response.status)
        }

    @Test
    fun `api GET slug should fail if ANY criteria is met - max clicks reached`() =
        testApplication {
            val config = initDatabaseInSchema("criteria_clicks_expired")
            application {
                attributes.put(AppConfigKey, config)
                configureSerialization()
                configureErrorHandling()
                configureApiRoutes()
            }

            val client =
                createClient {
                    install(ContentNegotiation) {
                        json()
                    }
                    followRedirects = false
                }

            // Create a link that has 1 max click and is not expired by time
            val shortenResponse =
                client.post("/api/v1/shorten") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ShortenRequest(
                            url = "https://kotlinlang.org",
                            expiresAt = OffsetDateTime.now().plusDays(1).toString(),
                            maxClicks = 1,
                        ),
                    )
                }
            val slug =
                shortenResponse.bodyAsText().let {
                    it.substringAfter("\"slug\":\"").substringBefore("\"")
                }

            // First click - OK
            val response1 = client.get("/api/v1/$slug")
            assertEquals(HttpStatusCode.Found, response1.status)

            // Second click - Should fail
            val response2 = client.get("/api/v1/$slug")
            assertEquals(HttpStatusCode.Gone, response2.status)
        }
}
