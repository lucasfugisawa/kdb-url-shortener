package dev.kotlinbr.utlshortener.app.services

import dev.kotlinbr.utlshortener.infrastructure.repository.LinksRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlugGeneratorTest {
    @Test
    fun `generate returns base62 slug with default length when first is unique`() {
        val repo = mockk<LinksRepository>()
        every { repo.existsBySlug(any()) } returns false

        val generator = SlugGenerator(repo)
        val slug = generator.generate()

        assertEquals(7, slug.length)
        assertTrue(slug.matches(Regex("^[0-9a-zA-Z]{7}$")), "Slug must be base62 and length 7")
        // Should check existence exactly once with the generated slug
        verify(exactly = 1) { repo.existsBySlug(slug) }
    }

    @Test
    fun `generate returns base62 slug with default length when third is unique`() {
        val repo = mockk<LinksRepository>()
        every { repo.existsBySlug(any()) }.returnsMany(true, true, false)

        val generator = SlugGenerator(repo)
        val slug = generator.generate()

        assertEquals(7, slug.length)
        assertTrue(slug.matches(Regex("^[0-9a-zA-Z]{7}$")), "Slug must be base62 and length 7")
        // Should check existence exactly once with the generated slug
        verify(exactly = 3) { repo.existsBySlug(any()) }
    }

    @Test
    fun `should throw error if maxRetries exceeded`() {
        val repo = mockk<LinksRepository>()
        // Always collides
        every { repo.existsBySlug(any()) } returns true

        val gen = SlugGenerator(repo, length = 7, maxRetries = 3)

        val ex =
            assertThrows(IllegalStateException::class.java) {
                gen.generate()
            }
        assertTrue(ex.message!!.contains("after 3 attempts"))
        verify(exactly = 3) { repo.existsBySlug(any()) }
    }

    @Test
    fun `respects length and maxRetries passed in constructor`() {
        val repo = mockk<LinksRepository>()
        every { repo.existsBySlug(any()) } returns false

        val length = 10
        val maxRetries = 3
        val generator = SlugGenerator(repo, length = length, maxRetries = maxRetries)
        val slug = generator.generate()

        assertEquals(length, slug.length)
        verify(exactly = 1) { repo.existsBySlug(any()) }
    }
}
