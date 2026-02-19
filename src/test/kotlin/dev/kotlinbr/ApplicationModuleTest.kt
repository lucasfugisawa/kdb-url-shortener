package dev.kotlinbr

import dev.kotlinbr.utlshortener.app.config.AppConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.koin.ktor.ext.inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationModuleTest {
    @Test
    fun `injects AppConfig with default env dev`() =
        testApplication {
            // Ensure we don't touch DB during this test run
            System.setProperty("APP_SKIP_DB", "true")
            // Explicitly use the real module
            application {
                module()
                val cfg by inject<AppConfig>()
                assertEquals("dev", cfg.env, "Default env should be 'dev' unless overridden")
            }
        }

    @Test
    fun `respects app flags skipDb for readiness`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            // Also disable migrations explicitly for safety
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            val health = client.get("/health")
            assertEquals(HttpStatusCode.OK, health.status)
            assertEquals("{\"status\":\"ok\"}", health.bodyAsText())

            val ready = client.get("/health/ready")
            assertEquals(HttpStatusCode.ServiceUnavailable, ready.status)
            assertEquals("{\"status\":\"not-ready\"}", ready.bodyAsText())
        }

    @Test
    fun `calls HTTP Serialization and Routing configuration exposing core endpoints`() =
        testApplication {
            System.setProperty("APP_SKIP_DB", "true")
            System.setProperty("APP_RUN_MIGRATIONS", "false")

            application { module() }

            // GET / (now returns index.html)
            val root = client.get("/")
            assertEquals(HttpStatusCode.OK, root.status)
            assertTrue(root.bodyAsText().contains("<title>Encurtador de URL</title>"))

            // Health endpoint from InfraRoutes
            val health = client.get("/health")
            assertEquals(HttpStatusCode.OK, health.status)
            assertEquals("{\"status\":\"ok\"}", health.bodyAsText())
        }
}
