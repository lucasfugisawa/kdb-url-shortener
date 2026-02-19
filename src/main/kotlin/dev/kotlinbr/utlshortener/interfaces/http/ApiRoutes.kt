package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.app.services.UrlValidator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.PagedResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.StatsResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

/**
 * API endpoints.
 */
fun Application.configureApiRoutes() {
    val logger = LoggerFactory.getLogger("dev.kotlinbr.utlshortener.interfaces.http.ApiRoutes")
    val linksRepository by inject<LinksRepository>()
    val config by inject<AppConfig>()

    routing {
        // Redirection should be top-level for business logic
        get("/{slug}") {
            val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")
            if (slug.contains(".")) return@get
            handleRedirection(slug, linksRepository, logger)
        }

        route("/api/v1") {
            get("/{slug}") {
                val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")
                handleRedirection(slug, linksRepository, logger)
            }
            get("/{slug}/stats") {
                val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")
                val link = linksRepository.findBySlug(slug) ?: throw SlugNotFoundException("Link not found")

                call.respond(StatsResponse(slug = slug, clicks = link.clicksCount))
            }
            get("/links") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 25

                val (links, total) = linksRepository.findAll(page, size)
                val response =
                    PagedResponse(
                        items = links.map { it.toResponse() },
                        total = total,
                        page = page,
                        size = size,
                    )
                call.respond(response)
            }
            post("/shorten") {
                val shortenCreate = call.receive<ShortenRequest>()

                val url =
                    UrlValidator.validateAndNormalize(
                        shortenCreate.url,
                        allowLocalhost = config.flags.allowLocalhost,
                    )

                val slugGenerator =
                    SlugGenerator(
                        repo = linksRepository,
                        length = config.slug.length,
                        maxRetries = config.slug.maxRetries,
                    )
                val slug = slugGenerator.generate()

                val expiresAt =
                    try {
                        shortenCreate.expiresAt?.let { OffsetDateTime.parse(it) }
                    } catch (_: Exception) {
                        throw BadRequestException("Invalid date format.")
                    }

                val link =
                    Link(
                        slug = slug,
                        targetUrl = url,
                        expiresAt = expiresAt,
                        maxClicks = shortenCreate.maxClicks,
                    )

                linksRepository.save(link)
                val response = ShortenResponse(slug, "/$slug")
                call.respond(response)
            }
        }
    }
}

private suspend fun RoutingContext.handleRedirection(
    slug: String,
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
) {
    val link =
        try {
            linksRepository.findBySlug(slug)
        } catch (e: Exception) {
            logger.warn(
                "Error fetching slug {}: {}. Database probably not initialized.",
                slug,
                e.message,
            )
            null
        }

    val now = OffsetDateTime.now()
    val isExpired = link?.expiresAt != null && link.expiresAt.isBefore(now)
    val reachedMaxClicks = link?.maxClicks != null && link.clicksCount >= link.maxClicks

    if (link == null) {
        throw SlugNotFoundException("Slug $slug not found.")
    }

    if (!link.isActive || isExpired || reachedMaxClicks) {
        logger.info(
            "Redirect failed for slug: {}. Reason: {}",
            slug,
            if (!link.isActive) {
                "inactive"
            } else if (isExpired) {
                "expired by time"
            } else {
                "expired by clicks (${link.clicksCount}/${link.maxClicks})"
            },
        )
        throw LinkExpiredException("The link for slug $slug has expired or is inactive.")
    }

    logger.info("Redirecting slug {} to {}", slug, link.targetUrl)
    try {
        linksRepository.incrementClicks(slug)
    } catch (e: Exception) {
        logger.error("Error incrementing clicks for slug {}: {}", slug, e.message)
    }
    call.respondRedirect(link.targetUrl)
}
