package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * API endpoints.
 */
fun Application.configureApiRoutes() {
    routing {
        route("/api/v1") {
            get("/links") {
                val links = LinksRepository().findAll()
                val response = links.map { it.toResponse() }
                call.respond(response)
            }
        }
    }
}
