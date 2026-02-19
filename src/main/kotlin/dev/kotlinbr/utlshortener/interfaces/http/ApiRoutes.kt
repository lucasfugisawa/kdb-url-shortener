package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.app.services.UrlValidator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.LinkExpiredException
import dev.kotlinbr.utlshortener.interfaces.http.SlugNotFoundException
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.StatsResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
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
    val linksRepository = LinksRepository()

    routing {
        route("/api/v1") {
            get("/{slug}") {
                val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")

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
            get("/{slug}/stats") {
                val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")
                val link = linksRepository.findBySlug(slug) ?: throw SlugNotFoundException("Link not found")

                call.respond(StatsResponse(slug = slug, clicks = link.clicksCount))
            }
            get("/links") {
                val links = linksRepository.findAll()
                val response = links.map { it.toResponse() }
                call.respond(response)
            }
            post("/shorten") {
                val config = call.application.attributes[AppConfigKey]
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
                    } catch (e: Exception) {
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
