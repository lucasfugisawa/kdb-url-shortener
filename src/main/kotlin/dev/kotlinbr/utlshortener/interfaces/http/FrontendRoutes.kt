package dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Frontend endpoint(s).
 */
fun Application.configureFrontendRoutes() {
    routing {
        get("/") {
            val content =
                javaClass.classLoader
                    .getResourceAsStream("public/index.html")
                    ?.bufferedReader()
                    ?.readText()
            if (content != null) {
                call.respondText(content, ContentType.Text.Html)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/docs") {
            val html =
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>Swagger UI</title>
                  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
                </head>
                <body>
                  <div id="swagger-ui"></div>
                  <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                  <script>
                    window.onload = () => {
                      window.ui = SwaggerUIBundle({
                        url: '/openapi.yaml',
                        dom_id: '#swagger-ui',
                      });
                    };
                  </script>
                </body>
                </html>
                """.trimIndent()
            call.respondText(html, ContentType.Text.Html)
        }

        get("/openapi.yaml") {
            val content =
                javaClass.classLoader
                    .getResourceAsStream("openapi.yaml")
                    ?.bufferedReader()
                    ?.readText()
            if (content != null) {
                call.respondText(content, ContentType.parse("text/yaml"))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        staticResources("/", "public", index = "index.html")
    }
}
