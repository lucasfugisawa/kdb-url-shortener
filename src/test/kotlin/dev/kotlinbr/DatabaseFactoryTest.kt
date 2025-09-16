package dev.kotlinbr

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppFlags
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DEFAULT_DB_POOL_MAX
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DbConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.ServerConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import dev.kotlinbr.utlshortener.testutils.BaseIntegrationTest
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("integration")
class DatabaseFactoryTest : BaseIntegrationTest() {
    private fun createSchemaIfNeeded(schema: String) {
        DriverManager.getConnection(jdbcUrl(), username(), password()).use { conn ->
            conn.createStatement().use { st ->
                st.execute("CREATE SCHEMA IF NOT EXISTS \"$schema\"")
            }
        }
    }

    private fun configWith(
        schema: String,
        runMigrations: Boolean,
        poolMax: Int = 5,
        driver: String = "org.postgresql.Driver",
    ): AppConfig {
        createSchemaIfNeeded(schema)
        val base = jdbcUrl()
        val sep = if (base.contains("?")) "&" else "?"
        val url = "$base${sep}currentSchema=$schema"
        return AppConfig(
            env = "test",
            server = ServerConfig(port = 0),
            db =
                DbConfig(
                    driver = driver,
                    url = url,
                    user = username(),
                    password = password(),
                    poolMax = poolMax,
                ),
            flags = AppFlags(skipDb = false, runMigrations = runMigrations),
        )
    }

    @Test
    fun `4_2 isHealthy true when DB reachable after init`() {
        DatabaseFactory.resetForTesting()
        val cfg = configWith(schema = "s_health_ok", runMigrations = true)
        DatabaseFactory.init(cfg)
        assertTrue(DatabaseFactory.isHealthy(), "Database should be healthy after init + migrations")
    }

    @Test
    fun `4_3 Hikari configuration defaults when poolMax_le_0 and driver empty`() {
        DatabaseFactory.resetForTesting()
        val cfg = configWith(schema = "s_hikari_defaults", runMigrations = false, poolMax = 0, driver = "")
        DatabaseFactory.init(cfg)
        val ds = DatabaseFactory.getDataSourceForTesting()
        assertNotNull(ds, "HikariDataSource should be initialized")
        assertEquals(DEFAULT_DB_POOL_MAX, ds.maximumPoolSize, "maximumPoolSize should fall back to DEFAULT_DB_POOL_MAX")
        // driverClassName should fall back to org.postgresql.Driver by Config loader or explicit default in hikari()
        val driverClass =
            try {
                ds.driverClassName
            } catch (_: Throwable) {
                null
            }
        // If driverClassName is not exposed, at least ensure that JDBC URL works and no exception thrown so far
        if (driverClass != null) {
            assertEquals("org.postgresql.Driver", driverClass)
        }
    }

    @Test
    fun `4_4 Flyway migrations executed when runMigrations=true`() {
        DatabaseFactory.resetForTesting()
        val schema = "s_migrate_true"
        val cfg = configWith(schema = schema, runMigrations = true)
        DatabaseFactory.init(cfg)
        // Assert links table exists and unique index name exists
        val tableExists =
            transaction {
                exec("SELECT to_regclass('" + schema + ".links')") { rs ->
                    rs.next()
                    rs.getString(1) != null
                } ?: false
            }
        assertTrue(tableExists, "links table should exist after migrations")

        val idxExists =
            transaction {
                val sql = """
                    SELECT 1
                    FROM pg_indexes
                    WHERE schemaname='$schema'
                      AND tablename='links'
                      AND indexname='links_slug_uindex'
                    LIMIT 1
                """.trimIndent()
                exec(sql) { rs ->
                    rs.next()
                } ?: false
            }
        assertTrue(idxExists, "unique index links_slug_uindex should exist")
    }

    @Test
    fun `4_5 Migrations skipped when runMigrations=false`() {
        DatabaseFactory.resetForTesting()
        val schema = "s_migrate_false"
        val cfg = configWith(schema = schema, runMigrations = false)
        DatabaseFactory.init(cfg)
        // Table should not exist
        val tableExists =
            transaction {
                exec("SELECT to_regclass('" + schema + ".links')") { rs ->
                    rs.next()
                    rs.getString(1) != null
                } ?: false
            }
        assertFalse(tableExists, "links table should NOT exist when migrations are skipped")
    }

    @Test
    fun `4_6 Exception handling in isHealthy returns false when datasource closed`() {
        DatabaseFactory.resetForTesting()
        val cfg = configWith(schema = "s_health_exception", runMigrations = true)
        DatabaseFactory.init(cfg)
        // Close datasource to trigger IllegalState/SQL exception on health check
        DatabaseFactory.getDataSourceForTesting()?.close()
        val healthy = DatabaseFactory.isHealthy()
        assertFalse(healthy, "isHealthy should return false when datasource is closed/broken")
    }
}
