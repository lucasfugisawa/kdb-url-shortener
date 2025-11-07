package dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureRouting() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            // For validation errors where we want to preserve the custom message as plain text
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "Invalid request")
        }
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

    configureFrontendRoutes()
    configureInfraRoutes()
    configureApiRoutes()
}
