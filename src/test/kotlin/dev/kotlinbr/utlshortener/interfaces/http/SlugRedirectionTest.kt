package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.module
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import dev.kotlinbr.utlshortener.testutils.TestDataFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SlugRedirectionTest {
    private fun setDbProps() {
        System.setProperty("db.driver", "org.h2.Driver")
        System.setProperty("db.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        System.setProperty("db.user", "sa")
        System.setProperty("db.password", "")
    }

    @Test
    fun `should redirect from root slug to target URL`() =
        testApplication {
            setDbProps()
            val target = "https://kotlinlang.org"
            val slug = "kotlin"

            application {
                module()
                transaction {
                    LinksTable.deleteAll()
                    TestDataFactory.insertLink(TestDataFactory.buildLink(slug = slug, targetUrl = target))
                }
            }

            val response =
                createClient {
                    followRedirects = false
                }.get("/$slug")

            assertEquals(HttpStatusCode.Found, response.status)
            assertEquals(target, response.headers[HttpHeaders.Location])
        }

    @Test
    fun `should still reach existing infra routes`() =
        testApplication {
            setDbProps()
            application {
                module()
            }

            val response = client.get("/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
        }

    @Test
    fun `should still reach existing frontend routes`() =
        testApplication {
            setDbProps()
            application {
                module()
            }

            val response = client.get("/docs")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Html.withCharset(Charsets.UTF_8), response.contentType())
        }

    @Test
    fun `should still reach static resources`() =
        testApplication {
            setDbProps()
            application {
                module()
            }

            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `should return 404 for non-existent slug`() =
        testApplication {
            setDbProps()
            application {
                module()
            }

            val response = client.get("/nonexistent-slug-123")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
}
