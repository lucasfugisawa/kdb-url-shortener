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
            // GET /api/v1/{slug} -> 302 redirect to targetUrl when active and not expired; otherwise 404
            get("/{slug}") {
                val slug = call.parameters["slug"]?.trim().orEmpty()

                if (slug.isEmpty()) {
                    call.application.environment.log
                        .info("slug_missing -> 404")
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                val repo = LinksRepository()
                val link = repo.findBySlug(slug)

                val now = OffsetDateTime.now()
                val isExpired = link?.expiresAt?.let { !it.isAfter(now) } ?: false
                val isInactive = link?.isActive == false

                if (link == null || isInactive || isExpired) {
                    call.application.environment.log.info(
                        "slug_redirect_not_found slug=$slug reason=" +
                            when {
                                link == null -> "not_found"
                                isInactive -> "inactive"
                                isExpired -> "expired"
                                else -> "unknown"
                            },
                    )
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.application.environment.log
                    .info("slug_redirect_found slug=$slug target=${link.targetUrl}")
                call.respondRedirect(url = link.targetUrl, permanent = false)
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
