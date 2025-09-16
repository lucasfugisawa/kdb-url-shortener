package dev.kotlinbr

import dev.kotlinbr.dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http.dto.LinkResponse
import dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http.dto.toDomain
import dev.kotlinbr.dev.kotlinbr.utlshortener.interfaces.http.dto.toResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerializationMappingTest {
    @Test
    fun `6_1 Link_toResponse mapping preserves fields and ISO strings`() {
        val created: OffsetDateTime = OffsetDateTime.of(2023, 5, 1, 12, 34, 56, 0, ZoneOffset.UTC)
        val expires: OffsetDateTime = created.plusDays(30)
        val link =
            Link(
                id = 123L,
                slug = "abc123",
                targetUrl = "https://example.com/abc123",
                createdAt = created,
                isActive = true,
                expiresAt = expires,
            )

        val resp: LinkResponse = link.toResponse()

        assertEquals(link.id, resp.id)
        assertEquals(link.slug, resp.slug)
        assertEquals(link.targetUrl, resp.targetUrl)
        // Ensure ISO-8601 text matches OffsetDateTime.toString()
        assertEquals(created.toString(), resp.createdAt)
        assertEquals(link.isActive, resp.isActive)
        assertEquals(expires.toString(), resp.expiresAt)

        // Also test with null expiresAt
        val linkNoExp = link.copy(expiresAt = null)
        val respNoExp = linkNoExp.toResponse()
        assertNull(respNoExp.expiresAt)
        assertEquals(linkNoExp.createdAt.toString(), respNoExp.createdAt)
    }

    @Test
    fun `6_2 LinkResponse_toDomain parses ISO-8601 strings including nullable expiresAt`() {
        val created = "2024-02-03T04:05:06Z"
        val expires = "2024-03-04T05:06:07+02:00"

        val resp =
            LinkResponse(
                id = 7L,
                slug = "slug-7",
                targetUrl = "https://t.example/7",
                createdAt = created,
                isActive = false,
                expiresAt = expires,
            )

        val domain: Link = resp.toDomain()
        assertEquals(resp.id, domain.id)
        assertEquals(resp.slug, domain.slug)
        assertEquals(resp.targetUrl, domain.targetUrl)
        assertEquals(OffsetDateTime.parse(created), domain.createdAt)
        assertEquals(resp.isActive, domain.isActive)
        assertEquals(OffsetDateTime.parse(expires), domain.expiresAt)

        // Null expiresAt case
        val respNull = resp.copy(expiresAt = null)
        val domainNull = respNull.toDomain()
        assertNull(domainNull.expiresAt)
    }

    @Test
    fun `6_3 JSON serialization of LinkResponse and round-trip`() {
        val created = "2025-01-02T03:04:05Z"
        val resp =
            LinkResponse(
                id = 99L,
                slug = "zz",
                targetUrl = "https://zz.example/",
                createdAt = created,
                isActive = true,
                expiresAt = null,
            )

        val json = Json { encodeDefaults = true }
        val encoded: String = json.encodeToString(LinkResponse.serializer(), resp)

        // Parse to object to assert field names; expiresAt may be absent when null depending on explicitNulls default
        val obj: JsonObject = json.parseToJsonElement(encoded).jsonObject
        assertTrue("id" in obj && "slug" in obj && "targetUrl" in obj && "createdAt" in obj && "isActive" in obj)
        // expiresAt is optional; if present and null, it should be null literal
        if ("expiresAt" in obj) {
            assertTrue(obj["expiresAt"]!!.jsonPrimitive.isString.not()) // null literal, not a string
        }
        assertEquals("99", obj["id"]!!.jsonPrimitive.content)
        assertEquals("zz", obj["slug"]!!.jsonPrimitive.content)
        assertEquals("https://zz.example/", obj["targetUrl"]!!.jsonPrimitive.content)
        assertEquals(created, obj["createdAt"]!!.jsonPrimitive.content)
        assertEquals("true", obj["isActive"]!!.jsonPrimitive.content)

        // Round trip
        val decoded: LinkResponse = json.decodeFromString(LinkResponse.serializer(), encoded)
        assertEquals(resp, decoded)
    }
}
