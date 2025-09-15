package dev.kotlinbr.interfaces.http

import dev.kotlinbr.app.config.AppConfigKey
import dev.kotlinbr.infrastructure.db.DatabaseFactory
import dev.kotlinbr.infrastructure.repository.LinksRepository
import dev.kotlinbr.interfaces.http.dto.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val body = mapOf("code" to "internal_error", "message" to (cause.message ?: "Internal Server Error"))
            call.respond(HttpStatusCode.InternalServerError, body)
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, mapOf("code" to "not_found", "message" to "Resource not found"))
        }
        status(HttpStatusCode.BadRequest) { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("code" to "bad_request", "message" to "Invalid request"))
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

        route("/api/v1") {
            get("/links") {
                val links = LinksRepository().findAll()
                val response = links.map { it.toResponse() }
                call.respond(response)
            }
        }
    }
}
