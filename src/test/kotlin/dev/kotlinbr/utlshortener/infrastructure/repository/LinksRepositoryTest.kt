package dev.kotlinbr.utlshortener.infrastructure.repository

import dev.kotlinbr.utlshortener.domain.Link
import dev.kotlinbr.utlshortener.infrastructure.db.tables.LinksTable
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import dev.kotlinbr.utlshortener.testutils.TestClockUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("integration")
class LinksRepositoryTest : BaseIntegrationTest() {
    @Test
    fun `findAll returns rows mapped correctly`() {
        val schema = "s_repo_findall"
        initDatabaseInSchema(schema)

        val created1: OffsetDateTime = TestClockUtils.now()
        val created2: OffsetDateTime = TestClockUtils.now().plusDays(1)

        transaction {
            LinksTable.insert {
                it[slug] = "a1"
                it[targetUrl] = "https://a.example/1"
                it[createdAt] = created1
                it[isActive] = true
                it[expiresAt] = null
            }
            LinksTable.insert {
                it[slug] = "b2"
                it[targetUrl] = "https://b.example/2"
                it[createdAt] = created2
                it[isActive] = false
                it[expiresAt] = created2.plusDays(30)
            }
        }

        val repo = LinksRepository()
        val list: List<Link> = repo.findAll()
        assertEquals(2, list.size)

        val a = list.first { it.slug == "a1" }
        assertEquals("https://a.example/1", a.targetUrl)
        assertEquals(created1, a.createdAt)
        assertEquals(true, a.isActive)
        assertEquals(null, a.expiresAt)

        val b = list.first { it.slug == "b2" }
        assertEquals("https://b.example/2", b.targetUrl)
        assertEquals(created2, b.createdAt)
        assertEquals(false, b.isActive)
        assertEquals(created2.plusDays(30), b.expiresAt)
    }

    @Test
    fun `unique slug constraint enforced`() {
        val schema = "s_repo_unique"
        initDatabaseInSchema(schema)

        // First insert should succeed
        transaction {
            LinksTable.insert {
                it[slug] = "dup"
                it[targetUrl] = "https://dup.example/1"
                it[createdAt] = TestClockUtils.now()
                it[isActive] = true
                it[expiresAt] = null
            }
        }

        // Second insert with same slug should fail at DB level
        assertFails("Duplicate slug should violate unique constraint") {
            transaction {
                LinksTable.insert {
                    it[slug] = "dup"
                    it[targetUrl] = "https://dup.example/2"
                    it[createdAt] = TestClockUtils.now()
                    it[isActive] = true
                    it[expiresAt] = null
                }
            }
        }
    }

    @Test
    fun `existsBySlug returns true when slug exists`() {
        val schema = "s_repo_existsBySlug_true"
        initDatabaseInSchema(schema)

        val created1: OffsetDateTime = TestClockUtils.now()
        val created2: OffsetDateTime = TestClockUtils.now().plusDays(1)

        transaction {
            LinksTable.insert {
                it[slug] = "a1"
                it[targetUrl] = "https://a.example/1"
                it[createdAt] = created1
                it[isActive] = true
                it[expiresAt] = null
            }
            LinksTable.insert {
                it[slug] = "b2"
                it[targetUrl] = "https://b.example/2"
                it[createdAt] = created2
                it[isActive] = false
                it[expiresAt] = created2.plusDays(30)
            }
        }

        val repo = LinksRepository()
        val exists = repo.existsBySlug("a1")

        assertTrue(exists)
    }

    @Test
    fun `existsNotBySlug returns false when not slug exists`() {
        val schema = "s_repo_existsBySlug_false"
        initDbInSchema(schema)

        val created1: OffsetDateTime = TestClockUtils.now()
        val created2: OffsetDateTime = TestClockUtils.now().plusDays(1)

        transaction {
            LinksTable.insert {
                it[slug] = "Z9"
                it[targetUrl] = "https://a.example/255"
                it[createdAt] = created1
                it[isActive] = false
                it[expiresAt] = null
            }
            LinksTable.insert {
                it[slug] = "Z10"
                it[targetUrl] = "https://b.example/8222"
                it[createdAt] = created2
                it[isActive] = false
                it[expiresAt] = created2.plusDays(30)
            }
        }

        val repo = LinksRepository()
        val exists = repo.existsBySlug("a1")
        assertFalse(exists)
    }

    @Test
    fun `existsBySlug returns false when Repository is empty`() {
        val schema = "s_repo_empty_existsBySlug_false"
        initDbInSchema(schema)

        val repo = LinksRepository()
        val exists = repo.existsBySlug("a1")
        assertFalse(exists)
    }
}
