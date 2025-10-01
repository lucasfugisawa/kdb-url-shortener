package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.OffsetDateTime

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

            post("/shorten") {
                val shortenCreate = call.receive<ShortenRequest>()
                val linksRepository = LinksRepository()
                val slugGenerator = SlugGenerator(linksRepository)
                val slug = slugGenerator.generate()
                val link =
                    Link(
                        id = null,
                        slug = slug,
                        targetUrl = shortenCreate.url,
                        createdAt = OffsetDateTime.now(),
                        isActive = true,
                        expiresAt = null,
                    )
                linksRepository.save(link)
                val resposes = ShortenResponse(slug, "/$slug")
                call.respond(resposes)
            }
        }
    }
}
