package dev.kotlinbr.utlshortener.app.services

import dev.kotlinbr.utlshortener.interfaces.http.UrlInvalidException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UrlValidatorTest {
    @Test
    fun `should normalize url with trim`() {
        val input = "  https://google.com  "
        val expected = "https://google.com"
        assertEquals(expected, UrlValidator.validateAndNormalize(input))
    }

    @Test
    fun `should prefix with https if starts with www`() {
        val input = "www.kotlin.link"
        val expected = "https://www.kotlin.link"
        assertEquals(expected, UrlValidator.validateAndNormalize(input))
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://google.com", "https://google.com", "https://sub.domain.com/path?q=1"])
    fun `should accept valid http and https urls`(url: String) {
        assertEquals(url, UrlValidator.validateAndNormalize(url))
    }

    @ParameterizedTest
    @ValueSource(strings = ["ftp://google.com", "mailto:test@test.com", "javascript:alert(1)", "file:///etc/passwd"])
    fun `should reject invalid schemes`(url: String) {
        assertThrows<UrlInvalidException> {
            UrlValidator.validateAndNormalize(url)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://localhost", "http://127.0.0.1", "https://10.0.0.1", "http://192.168.1.1"])
    fun `should reject local hosts by default`(url: String) {
        assertThrows<UrlInvalidException> {
            UrlValidator.validateAndNormalize(url)
        }
    }

    @Test
    fun `should allow localhost if configured`() {
        val url = "http://localhost"
        assertEquals(url, UrlValidator.validateAndNormalize(url, allowLocalhost = true))
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://google", "https://my-server", "http://domain."])
    fun `should reject domains without plausible TLD`(url: String) {
        assertThrows<UrlInvalidException> {
            UrlValidator.validateAndNormalize(url)
        }
    }

    @Test
    fun `should reject empty url`() {
        assertThrows<UrlInvalidException> {
            UrlValidator.validateAndNormalize("   ")
        }
    }

    @Test
    fun `should reject too long url`() {
        val longUrl = "https://google.com/" + "a".repeat(2001)
        assertThrows<UrlInvalidException> {
            UrlValidator.validateAndNormalize(longUrl)
        }
    }
}
