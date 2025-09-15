package dev.kotlinbr

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorFormatTest {
    @Test
    fun not_found_returns_code_and_message() =
        testApplication {
            application {
                System.setProperty("APP_SKIP_DB", "true")
                module()
            }
            val response: HttpResponse = client.get("/non-existent-path")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
            val body = response.bodyAsText()
            assertTrue(body.contains("\"code\":"))
            assertTrue(body.contains("\"message\":"))
        }
}
