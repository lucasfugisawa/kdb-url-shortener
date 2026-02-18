package dev.kotlinbr.utlshortener.interfaces.http

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
import dev.kotlinbr.utlshortener.app.services.SlugGenerator
import dev.kotlinbr.utlshortener.app.services.UrlValidator
import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenRequest
import dev.kotlinbr.utlshortener.interfaces.http.dto.ShortenResponse
import dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
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

                val html404 =
                    """
                    <!DOCTYPE html>
                    <html lang="pt-BR">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Página não encontrada - 404</title>
                        <style>
                            body {
                                font-family: sans-serif;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                height: 100vh;
                                margin: 0;
                                background-color: #f8f9fa;
                                text-align: center;
                            }
                            .container {
                                max-width: 500px;
                                padding: 40px;
                                background: white;
                                border-radius: 8px;
                                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                            }
                            h1 { color: #dc3545; font-size: 48px; margin-bottom: 20px; }
                            p { color: #6c757d; font-size: 18px; margin-bottom: 30px; }
                            a {
                                display: inline-block;
                                padding: 12px 24px;
                                background-color: #007bff;
                                color: white;
                                text-decoration: none;
                                border-radius: 4px;
                                transition: background-color 0.2s;
                            }
                            a:hover { background-color: #0056b3; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1>Ops! 404</h1>
                            <p>O link que você está tentando acessar não existe, foi desativado ou expirou.</p>
                            <a href="/">Voltar para o Início</a>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()

                val link =
                    try {
                        val linksRepository = LinksRepository()
                        linksRepository.findBySlug(slug)
                    } catch (e: Exception) {
                        logger.warn(
                            "Erro ao buscar slug {}: {}. Provavelmente banco não inicializado.",
                            slug,
                            e.message,
                        )
                        null
                    }

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
                    call.response.header("Cache-Control", "no-store")
                    call.response.header("X-Friendly-404", "true")
                    call.respondText(html404, ContentType.Text.Html, HttpStatusCode.NotFound)
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
                val config = call.application.attributes[AppConfigKey]
                val shortenCreate = call.receive<ShortenRequest>()
                val url =
                    UrlValidator.validateAndNormalize(
                        shortenCreate.url,
                        allowLocalhost = config.flags.allowLocalhost,
                    )

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
