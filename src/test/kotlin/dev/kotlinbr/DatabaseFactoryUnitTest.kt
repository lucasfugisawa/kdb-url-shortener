package dev.kotlinbr

import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse

class DatabaseFactoryUnitTest {
    @AfterTest
    fun tearDown() {
        // Ensure clean state for other tests
        DatabaseFactory.resetForTesting()
    }

    @Test
    fun `4_1 isHealthy returns false when not initialized`() {
        // Ensure not initialized
        DatabaseFactory.resetForTesting()
        val healthy = DatabaseFactory.isHealthy()
        assertFalse(healthy, "isHealthy should be false before DatabaseFactory.init is called")
    }
}
