package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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
            post("/shorten") {
                val shortenCreate = call.receive<ShortenRequest>()
                val linksRepository = LinksRepository()
                val slugGenerator = SlugGenerator(linksRepository)
                val slug = slugGenerator.generate()
                val url = shortenCreate.url.trim()

                if (url.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "URL não pode estar vazia.")
                    return@post
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    call.respond(HttpStatusCode.BadRequest, "URL inválida. Use http:// ou https://")
                    return@post
                }

                val link =
                    Link(
                        slug = slug,
                        targetUrl = url,
                    )

                linksRepository.save(link)
                val response = ShortenResponse(slug, "/$slug")
                call.respond(response)
            }
        }
    }
}
