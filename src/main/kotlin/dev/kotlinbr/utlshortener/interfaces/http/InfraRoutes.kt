package dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfigKey
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Infrastructure/utility endpoints: health and env.
 */
fun Application.configureInfraRoutes() {
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
            val cfg = this@configureInfraRoutes.attributes[AppConfigKey]
            call.respond(mapOf("env" to cfg.env))
        }
    }
}
