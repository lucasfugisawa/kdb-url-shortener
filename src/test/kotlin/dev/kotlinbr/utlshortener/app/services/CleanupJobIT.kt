package dev.kotlinbr.utlshortener.app.services

import dev.kotlinbr.utlshortener.interfaces.http.configureRouting
import dev.kotlinbr.utlshortener.interfaces.http.configureSerialization
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import dev.kotlinbr.utlshortener.testutils.TestDataFactory
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class CleanupJobIT : BaseIntegrationTest() {
    @Test
    fun `should deactivate expired links when job runs`() =
        testApplication {
            // Setup database and schema for this test
            val schema = "cleanup_job_test"
            val appConfig = initDatabaseInSchema(schema)

            application {
                attributes.put(dev.kotlinbr.utlshortener.app.config.AppConfigKey, appConfig)
                val linksRepository =
                    dev.kotlinbr.utlshortener.infrastructure.repository
                        .LinksRepository()
                val cleanupJob = CleanupJob(linksRepository, appConfig.cleanupIntervalMinutes)
                attributes.put(dev.kotlinbr.utlshortener.app.config.CleanupJobKey, cleanupJob)

                configureSerialization()
                configureRouting()
            }

            // Create links with different conditions
            val expiredLink =
                TestDataFactory.buildLink(
                    slug = "expired",
                    expiresAt = OffsetDateTime.now().minusMinutes(1),
                    isActive = true,
                )
            val maxClicksReachedLink =
                TestDataFactory.buildLink(
                    slug = "max-clicks",
                    isActive = true,
                    clicksCount = 5,
                    maxClicks = 5,
                )
            val validLink =
                TestDataFactory.buildLink(
                    slug = "valid",
                    expiresAt = OffsetDateTime.now().plusDays(1),
                    isActive = true,
                    clicksCount = 0,
                    maxClicks = 10,
                )
            val alreadyInactiveLink =
                TestDataFactory.buildLink(
                    slug = "inactive",
                    isActive = false,
                    expiresAt = OffsetDateTime.now().minusDays(1),
                )

            TestDataFactory.insertLink(expiredLink)
            TestDataFactory.insertLink(maxClicksReachedLink)
            TestDataFactory.insertLink(validLink)
            TestDataFactory.insertLink(alreadyInactiveLink)

            // Trigger cleanup via endpoint
            val response = client.post("/admin/cleanup")

            if (response.status != HttpStatusCode.OK) {
                println("[DEBUG_LOG] Request failed with status: ${response.status}")
                println("[DEBUG_LOG] Response body: ${response.bodyAsText()}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"affected\":\"2\""))

            // Validate states
            val updatedExpired = TestDataFactory.findLinkBySlug("expired")!!
            val updatedMaxClicks = TestDataFactory.findLinkBySlug("max-clicks")!!
            val updatedValid = TestDataFactory.findLinkBySlug("valid")!!
            val updatedInactive = TestDataFactory.findLinkBySlug("inactive")!!

            assertFalse(updatedExpired.isActive, "Expired link should be inactive")
            assertFalse(updatedMaxClicks.isActive, "Max clicks reached link should be inactive")
            assertTrue(updatedValid.isActive, "Valid link should remain active")
            assertFalse(updatedInactive.isActive, "Already inactive link should remain inactive")
        }
}
