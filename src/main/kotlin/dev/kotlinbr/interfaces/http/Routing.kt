package dev.kotlinbr.interfaces.http

import dev.kotlinbr.app.config.AppConfigKey
import dev.kotlinbr.infrastructure.db.DatabaseFactory
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
            val cfg = this@configureRouting.attributes[AppConfigKey]
            call.respond(mapOf("env" to cfg.env))
        }
    }
}
