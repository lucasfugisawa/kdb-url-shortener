package dev.kotlinbr.app.http

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.MDC
import java.util.UUID

fun Application.configureHTTP() {
    install(Compression)

    intercept(ApplicationCallPipeline.Monitoring) {
        val startNs = System.nanoTime()
        val requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
        val path = call.request.path()
        val method = call.request.httpMethod.value

        MDC.put("correlationId", requestId)
        MDC.put("path", path)
        MDC.put("method", method)
        call.response.headers.append("X-Request-ID", requestId)

        this@configureHTTP.environment.log.info("request_started")

        try {
            proceed()
        } finally {
            val statusCode =
                call.response
                    .status()
                    ?.value
                    ?.toString() ?: "0"
            val latencyMs = (System.nanoTime() - startNs) / 1_000_000
            MDC.put("status", statusCode)
            MDC.put("latencyMs", latencyMs.toString())

            this@configureHTTP.environment.log.info("request_completed")

            MDC.clear()
        }
    }
}
