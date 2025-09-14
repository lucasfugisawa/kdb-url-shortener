package dev.kotlinbr

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.MDC
import java.util.*

fun Application.configureHTTP() {
    install(Compression)

    intercept(ApplicationCallPipeline.Monitoring) {
        val startNs = System.nanoTime()
        val requestId = call.request.headers["X-Request-ID"] ?: UUID.randomUUID().toString()
        val path = call.request.path()
        val method = call.request.httpMethod.value

        // Put initial MDC values
        MDC.put("correlationId", requestId)
        MDC.put("path", path)
        MDC.put("method", method)
        // Propagate header to response
        call.response.headers.append("X-Request-ID", requestId)

        // Log start of request handling
        this@configureHTTP.environment.log.info("request_started")

        try {
            proceed()
        } finally {
            val statusCode = call.response.status()?.value?.toString() ?: "0"
            val latencyMs = (System.nanoTime() - startNs) / 1_000_000
            MDC.put("status", statusCode)
            MDC.put("latencyMs", latencyMs.toString())

            // Log end of request handling
            this@configureHTTP.environment.log.info("request_completed")

            // Clear MDC to avoid leaking to next calls
            MDC.clear()
        }
    }
}
