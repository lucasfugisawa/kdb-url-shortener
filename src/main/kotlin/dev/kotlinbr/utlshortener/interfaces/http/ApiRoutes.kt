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
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val DEFAULT_PAGE_SIZE = 25

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
            handleSlugRedirection(linksRepository, logger)
        }

        route("/api/v1") {
            get("/{slug}") {
                handleApiSlugRedirection(linksRepository, logger)
            }
            get("/{slug}/stats") {
                handleGetStats(linksRepository)
            }
            get("/links") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

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
                handleShortenRequest(linksRepository, config)
            }
        }
    }
}

private suspend fun RoutingContext.handleSlugRedirection(
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
) {
    val slug = call.parameters["slug"] ?: return
    if (slug.contains(".")) return
    handleRedirection(slug, linksRepository, logger)
}

private suspend fun RoutingContext.handleApiSlugRedirection(
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
) {
    val slug = call.parameters["slug"] ?: throw SlugNotFoundException("Slug is required.")
    handleRedirection(slug, linksRepository, logger)
}

private suspend fun RoutingContext.handleGetStats(linksRepository: LinksRepository) {
    val slug = call.parameters["slug"] ?: throw BadRequestException("Slug is required.")
    val link = linksRepository.findBySlug(slug) ?: throw SlugNotFoundException("Link not found")

    call.respond(StatsResponse(slug = slug, clicks = link.clicksCount))
}

private suspend fun RoutingContext.handleShortenRequest(
    linksRepository: LinksRepository,
    config: AppConfig,
) {
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
        } catch (e: DateTimeParseException) {
            throw BadRequestException("Invalid date format.", e)
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

private suspend fun RoutingContext.handleRedirection(
    slug: String,
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
) {
    val link = findLink(slug, linksRepository, logger)
    val now = OffsetDateTime.now()
    val isExpired = link.expiresAt != null && link.expiresAt.isBefore(now)
    val reachedMaxClicks = link.maxClicks != null && link.clicksCount >= link.maxClicks

    if (!link.isActive || isExpired || reachedMaxClicks) {
        logRedirectFailure(slug, link, isExpired, logger)
        throw LinkExpiredException("The link for slug $slug has expired or is inactive.")
    }

    logger.info("Redirecting slug {} to {}", slug, link.targetUrl)
    incrementClicks(slug, linksRepository, logger)
    call.respondRedirect(link.targetUrl)
}

private fun findLink(
    slug: String,
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
): dev.kotlinbr.utlshortener.domain.Link {
    val link =
        try {
            linksRepository.findBySlug(slug)
        } catch (e: org.jetbrains.exposed.exceptions.ExposedSQLException) {
            logger.warn(
                "Error fetching slug {}: {}. Database probably not initialized.",
                slug,
                e.message,
            )
            null
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Database.connect()") == true) {
                logger.warn("Database not initialized for slug {}.", slug)
                null
            } else {
                throw e
            }
        }

    return link ?: throw SlugNotFoundException("Slug $slug not found.")
}

private suspend fun incrementClicks(
    slug: String,
    linksRepository: LinksRepository,
    logger: org.slf4j.Logger,
) {
    try {
        linksRepository.incrementClicks(slug)
    } catch (e: org.jetbrains.exposed.exceptions.ExposedSQLException) {
        logger.error("Error incrementing clicks for slug {}: {}", slug, e.message)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.error("Unexpected error incrementing clicks for slug {}: {}", slug, e.message)
    }
}

private fun logRedirectFailure(
    slug: String,
    link: dev.kotlinbr.utlshortener.domain.Link,
    isExpired: Boolean,
    logger: org.slf4j.Logger,
) {
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
}
