package dev.kotlinbr.utlshortener.app.services

import dev.kotlinbr.utlshortener.interfaces.http.UrlInvalidException
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
        val normalized = preprocessUrl(url)
        validateUrlLength(normalized)
        validateUrlNotEmpty(normalized)

        val uri = parseUri(normalized)
        validateScheme(uri)
        val host = validateHost(uri)

        validateLocalHost(host, allowLocalhost)
        validateTld(host, allowLocalhost)

        return normalized
    }

    private fun preprocessUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("www.", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun validateUrlLength(url: String) {
        if (url.length > MAX_URL_LENGTH) {
            throw UrlInvalidException("URL too long. Maximum of $MAX_URL_LENGTH characters.")
        }
    }

    private fun validateUrlNotEmpty(url: String) {
        if (url.isEmpty()) {
            throw UrlInvalidException("URL cannot be empty.")
        }
    }

    private fun parseUri(url: String): URI =
        try {
            URI(url)
        } catch (e: URISyntaxException) {
            throw UrlInvalidException("Invalid URL: ${e.message}", e)
        }

    private fun validateScheme(uri: URI) {
        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme !in VALID_SCHEMES) {
            throw UrlInvalidException("Invalid scheme. Use http:// or https://")
        }
    }

    private fun validateHost(uri: URI): String = uri.host?.lowercase() ?: throw UrlInvalidException("Invalid host.")

    private fun validateLocalHost(
        host: String,
        allowLocalhost: Boolean,
    ) {
        if (!allowLocalhost) {
            if (host in LOCAL_HOSTS || LOCAL_IP_RANGES.any { host.startsWith(it) }) {
                throw UrlInvalidException("Local URLs are not allowed.")
            }
        }
    }

    private fun validateTld(
        host: String,
        allowLocalhost: Boolean,
    ) {
        // Simple TLD check: must have at least one dot and something after it
        if (!host.contains(".") || host.substringAfterLast(".").isEmpty()) {
            // Exception for localhost if allowed, but we already handled host in LOCAL_HOSTS
            if (!allowLocalhost || host != "localhost") {
                throw UrlInvalidException("Domain must have a plausible TLD.")
            }
        }
    }
}
