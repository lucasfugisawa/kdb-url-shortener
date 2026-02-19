package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.utlshortener.app.services.CleanupJob
import dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Infrastructure/utility endpoints: health and env.
 */
fun Application.configureInfraRoutes() {
    val config by inject<AppConfig>()
    val cleanupJob by inject<CleanupJob>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        get("/health/ready") {
            if (DatabaseFactory.isHealthy()) {
                call.respond(mapOf("status" to "ready"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not-ready"))
            }
        }
        get("/env") {
            call.respond(mapOf("env" to config.env))
        }
        post("/admin/cleanup") {
            val affected = cleanupJob.runCleanup()
            call.respond(mapOf("status" to "success", "affected" to affected.toString()))
        }
    }
}
