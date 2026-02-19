package dev.kotlinbr.utlshortener.app.services

import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

class CleanupJob(
    private val linksRepository: LinksRepository,
    private val intervalMinutes: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        logger.info("Starting CleanupJob with interval of $intervalMinutes minutes")
        scope.launch {
            while (isActive) {
                try {
                    runCleanup()
                } catch (e: Exception) {
                    logger.error("Error during cleanup job execution", e)
                }
                delay(intervalMinutes.minutes)
            }
        }
    }

    fun runCleanup(): Int {
        logger.info("Running cleanup for expired links...")
        val affectedRows = linksRepository.deactivateExpiredLinks()
        logger.info("Cleanup finished. $affectedRows links deactivated.")
        return affectedRows
    }
}
