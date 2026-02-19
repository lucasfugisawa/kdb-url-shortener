package dev.kotlinbr.utlshortener.interfaces.http

import io.ktor.server.application.Application

fun Application.configureRouting() {
    configureErrorHandling()
    configureInfraRoutes()
    configureApiRoutes()
    configureFrontendRoutes()
}
