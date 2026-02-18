package dev.kotlinbr.utlshortener.app.services

import io.ktor.server.plugins.*
import java.net.URI
import java.net.URISyntaxException

/**
 * Service to validate and normalize URLs.
 */
object UrlValidator {
    private const val MAX_URL_LENGTH = 2000
    private val VALID_SCHEMES = setOf("http", "https")
    private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1")
    private val LOCAL_IP_RANGES =
        listOf(
            "10.",
            "192.168.",
            "172.16.",
            "172.17.",
            "172.18.",
            "172.19.",
            "172.20.",
            "172.21.",
            "172.22.",
            "172.23.",
            "172.24.",
            "172.25.",
            "172.26.",
            "172.27.",
            "172.28.",
            "172.29.",
            "172.30.",
            "172.31.",
        )

    /**
     * Normalizes and validates the URL.
     * Returns the normalized URL or throws a BadRequestException if invalid.
     */
    fun validateAndNormalize(
        url: String,
        allowLocalhost: Boolean = false,
    ): String {
        var normalized = url.trim()

        if (normalized.length > MAX_URL_LENGTH) {
            throw BadRequestException("URL muito longa. Máximo de $MAX_URL_LENGTH caracteres.")
        }

        if (normalized.isEmpty()) {
            throw BadRequestException("URL não pode estar vazia.")
        }

        // If it starts with www. without scheme, prefix with https://
        if (normalized.startsWith("www.", ignoreCase = true)) {
            normalized = "https://$normalized"
        }

        val uri =
            try {
                URI(normalized)
            } catch (e: URISyntaxException) {
                throw BadRequestException("URL inválida: ${e.message}")
            }

        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme !in VALID_SCHEMES) {
            throw BadRequestException("Esquema inválido. Use http:// ou https://")
        }

        val host = uri.host?.lowercase() ?: throw BadRequestException("Host inválido.")

        if (!allowLocalhost) {
            if (host in LOCAL_HOSTS || LOCAL_IP_RANGES.any { host.startsWith(it) }) {
                throw BadRequestException("URLs locais não são permitidas.")
            }
        }

        // Simple TLD check: must have at least one dot and something after it
        if (!host.contains(".") || host.substringAfterLast(".").isEmpty()) {
            // Exception for localhost if allowed, but we already handled host in LOCAL_HOSTS
            if (!allowLocalhost || host != "localhost") {
                throw BadRequestException("Domínio deve ter um TLD plausível.")
            }
        }

        return normalized
    }
}
