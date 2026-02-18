package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

/**
 * API endpoints.
 */
fun Application.configureApiRoutes() {
    val logger = LoggerFactory.getLogger("dev.kotlinbr.utlshortener.interfaces.http.ApiRoutes")
    routing {
        route("/api/v1") {
            get("/{slug}") {
                val slug = call.parameters["slug"] ?: throw BadRequestException("Slug é obrigatório.")
                val linksRepository = LinksRepository()
                val link = linksRepository.findBySlug(slug)

                val now = OffsetDateTime.now()
                if (link == null || !link.isActive || (link.expiresAt != null && link.expiresAt.isBefore(now))) {
                    logger.info(
                        "Redirect falhou para slug: {}. Motivo: {}",
                        slug,
                        if (link ==
                            null
                        ) {
                            "não encontrado"
                        } else if (!link.isActive) {
                            "inativo"
                        } else {
                            "expirado"
                        },
                    )
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                logger.info("Redirecting slug {} to {}", slug, link.targetUrl)
                call.respondRedirect(link.targetUrl)
            }
            get("/links") {
                val links = LinksRepository().findAll()
                val response = links.map { it.toResponse() }
                call.respond(response)
            }
            post("/shorten") {
                val shortenCreate = call.receive<ShortenRequest>()
                val url = shortenCreate.url.trim()

                if (url.isEmpty()) {
                    throw BadRequestException("URL não pode estar vazia.")
                }

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    throw BadRequestException("URL inválida. Use http:// ou https://")
                }

                val linksRepository = LinksRepository()
                val slugGenerator = SlugGenerator(linksRepository)
                val slug = slugGenerator.generate()

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
