package dev.kotlinbr

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testHealth() = testApplication {
        application {
            module()
        }
        val response: HttpResponse = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json.withCharset(Charsets.UTF_8), response.contentType())
        val body = response.bodyAsText().trim()
        assertEquals("{\"status\":\"ok\"}", body)
    }
}
