package dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

class UrlInvalidException(
    message: String,
) : RuntimeException(message)

class SlugNotFoundException(
    message: String,
) : RuntimeException(message)

class LinkExpiredException(
    message: String,
) : RuntimeException(message)

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<UrlInvalidException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("URL_INVALID", cause.message ?: "Invalid URL"))
        }
        exception<SlugNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("SLUG_NOT_FOUND", cause.message ?: "Slug not found"))
        }
        exception<LinkExpiredException> { call, _ ->
            val content =
                javaClass.classLoader
                    .getResourceAsStream("public/404.html")
                    ?.bufferedReader()
                    ?.readText()
            if (content != null) {
                call.respondText(content, ContentType.Text.Html, HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Link expired and 404 page not found"))
            }
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    "VALIDATION_ERROR",
                    cause.message ?: "Validation error",
                ),
            )
        }
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("NOT_FOUND", cause.message ?: "Resource not found"),
            )
        }
        exception<Throwable> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"),
            )
        }
    }
}
