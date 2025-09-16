package dev.kotlinbr

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppFlags
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DbConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.ServerConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Validates schema produced by Flyway migrations.
 */
@Tag("integration")
class SchemaValidationTest : BaseIntegrationTest() {
    private fun createSchemaIfNeeded(schema: String) {
        DriverManager.getConnection(jdbcUrl(), username(), password()).use { conn ->
            conn.createStatement().use { st ->
                st.execute("CREATE SCHEMA IF NOT EXISTS \"$schema\"")
            }
        }
    }

    private fun initDbInSchema(schema: String) {
        createSchemaIfNeeded(schema)
        val base = jdbcUrl()
        val sep = if (base.contains("?")) "&" else "?"
        val url = "$base${sep}currentSchema=$schema"
        val cfg =
            AppConfig(
                env = "test",
                server = ServerConfig(port = 0),
                db =
                    DbConfig(
                        driver = "org.postgresql.Driver",
                        url = url,
                        user = username(),
                        password = password(),
                        poolMax = 5,
                    ),
                flags = AppFlags(skipDb = false, runMigrations = true),
            )
        DatabaseFactory.resetForTesting()
        DatabaseFactory.init(cfg)
        // Ensure sane isolation level for direct queries in tests
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
    }

    @Test
    fun `7_1 links table columns and types from migration`() {
        val schema = "s_schema_validate_cols"
        initDbInSchema(schema)

        // Helper to fetch a column's metadata from information_schema
        data class Col(
            val name: String,
            val dataType: String?,
            val isNullable: String?,
            val charMax: Int?,
            val columnDefault: String?,
        )

        fun loadColumn(col: String): Col =
            transaction {
                exec(
                    """
                    SELECT column_name, data_type, is_nullable, character_maximum_length, column_default
                    FROM information_schema.columns
                    WHERE table_schema = '$schema' AND table_name = 'links' AND column_name = '$col'
                    """.trimIndent(),
                ) { rs ->
                    if (rs.next()) {
                        Col(
                            name = rs.getString("column_name"),
                            dataType = rs.getString("data_type"),
                            isNullable = rs.getString("is_nullable"),
                            charMax = rs.getObject("character_maximum_length")?.let { (it as Number).toInt() },
                            columnDefault = rs.getString("column_default"),
                        )
                    } else {
                        error("Column $col not found")
                    }
                }!!
            }

        // id BIGSERIAL PK (bigserial -> bigint with nextval default)
        val id = loadColumn("id")
        assertEquals("NO", id.isNullable, "id should be NOT NULL")
        // In PG, bigserial shows data_type = 'bigint'
        assertEquals("bigint", id.dataType)
        assertNotNull(id.columnDefault, "id should have nextval default (serial)")
        assertTrue(
            id.columnDefault!!.contains("nextval"),
            "id default should reference nextval: ${'$'}{id.columnDefault}",
        )

        // slug VARCHAR(32) NOT NULL
        val slug = loadColumn("slug")
        assertEquals("character varying", slug.dataType)
        assertEquals(32, slug.charMax)
        assertEquals("NO", slug.isNullable)

        // target_url TEXT NOT NULL
        val target = loadColumn("target_url")
        assertEquals("text", target.dataType)
        assertEquals("NO", target.isNullable)

        // created_at TIMESTAMPTZ NOT NULL DEFAULT now()
        val createdAt = loadColumn("created_at")
        assertEquals("timestamp with time zone", createdAt.dataType)
        assertEquals("NO", createdAt.isNullable)
        assertNotNull(createdAt.columnDefault)
        assertTrue(
            createdAt.columnDefault!!.contains("now"),
            "created_at default should reference now(): ${'$'}{createdAt.columnDefault}",
        )

        // is_active BOOLEAN NOT NULL DEFAULT TRUE
        val isActive = loadColumn("is_active")
        assertEquals("boolean", isActive.dataType)
        assertEquals("NO", isActive.isNullable)
        assertNotNull(isActive.columnDefault)
        assertTrue(isActive.columnDefault!!.contains("true", ignoreCase = true))

        // expires_at TIMESTAMPTZ NULL
        val expiresAt = loadColumn("expires_at")
        assertEquals("timestamp with time zone", expiresAt.dataType)
        assertEquals("YES", expiresAt.isNullable)
        assertNull(expiresAt.columnDefault)

        // Verify primary key on id
        val pkOk =
            transaction {
                exec(
                    """
                    SELECT kcu.column_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                     AND tc.table_schema = kcu.table_schema
                    WHERE tc.table_schema = '$schema' AND tc.table_name = 'links' AND tc.constraint_type = 'PRIMARY KEY'
                    """.trimIndent(),
                ) { rs ->
                    val cols = mutableListOf<String>()
                    while (rs.next()) cols.add(rs.getString("column_name"))
                    cols
                }!!.singleOrNull() == "id"
            }
        assertTrue(pkOk, "Primary key should be on column 'id'")

        // Optional: inserting without created_at should default to now()
        transaction {
            exec("INSERT INTO links(slug, target_url) VALUES ('defnow', 'https://example/def')")
        }
        val createdNotNull =
            transaction {
                exec("SELECT created_at, is_active, expires_at FROM links WHERE slug = 'defnow'") { rs ->
                    rs.next()
                    val created = rs.getTimestamp(1)
                    val active = rs.getBoolean(2)
                    val expires = rs.getTimestamp(3)
                    Triple(created, active, expires)
                }!!
            }
        assertNotNull(createdNotNull.first, "created_at should be auto-populated by default now()")
        assertTrue(createdNotNull.second, "is_active should default to true")
        assertNull(createdNotNull.third, "expires_at should remain NULL when not provided")
    }

    @Test
    fun `7_2 unique index on slug exists with specific name`() {
        val schema = "s_schema_validate_index"
        initDbInSchema(schema)

        // Check index by name and uniqueness using pg_catalog
        val existsAndUnique =
            transaction {
                exec(
                    """
                    SELECT ix.indisunique
                    FROM pg_class t
                    JOIN pg_index ix ON t.oid = ix.indrelid
                    JOIN pg_class i ON ix.indexrelid = i.oid
                    WHERE t.relname = 'links' AND i.relname = 'links_slug_uindex'
                    """.trimIndent(),
                ) { rs ->
                    if (rs.next()) rs.getBoolean(1) else null
                }
            }
        assertNotNull(existsAndUnique, "Index links_slug_uindex should exist on links table")
        assertTrue(existsAndUnique == true, "Index links_slug_uindex should be UNIQUE")
    }
}
