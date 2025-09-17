package dev.kotlinbr

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationModuleTest {
    @Test
    fun `injects AppConfig into attributes with default env dev`() =
        testApplication {
            // Ensure we don't touch DB during this test run
            System.setProperty("APP_SKIP_DB", "true")
            // Explicitly use the real module
            application {
                module()
                val cfg = attributes[AppConfigKey]
                assertNotNull(cfg, "AppConfig should be present in application attributes")
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

            // GET /
            val root = client.get("/")
            assertEquals(HttpStatusCode.OK, root.status)
            assertEquals("Hello World!", root.bodyAsText())

            // Health endpoint from InfraRoutes
            val health = client.get("/health")
            assertEquals(HttpStatusCode.OK, health.status)
            assertEquals("{\"status\":\"ok\"}", health.bodyAsText())
        }
}
