package dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Frontend endpoint(s).
 * For now, it just serves a placeholder at '/'.
 */
fun Application.configureFrontendRoutes() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
