package dev.kotlinbr

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Liveness
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        // Readiness: check DB connectivity
        get("/health/ready") {
            if (DatabaseFactory.isHealthy()) {
                call.respond(mapOf("status" to "ready"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not-ready"))
            }
        }
        // Expose current environment for testing/inspection
        get("/env") {
            val cfg = this@configureRouting.attributes[AppConfigKey]
            call.respond(mapOf("env" to cfg.env))
        }
    }
}
