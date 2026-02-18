package dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing

/**
 * Frontend endpoint(s).
 */
fun Application.configureFrontendRoutes() {
    routing {
        staticResources("/", "public", index = "index.html")
    }
}
