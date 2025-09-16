package dev.kotlinbr.utlshortener.app.config

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DEFAULT_SERVER_PORT
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.loadAppConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Disabled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigLoaderTest {
    private val touchedSysProps = mutableMapOf<String, String?>()

    private fun setProp(
        key: String,
        value: String,
    ) {
        if (!touchedSysProps.containsKey(key)) {
            touchedSysProps[key] = System.getProperty(key)
        }
        System.setProperty(key, value)
    }

    private fun clearProp(key: String) {
        if (!touchedSysProps.containsKey(key)) {
            touchedSysProps[key] = System.getProperty(key)
        }
        System.clearProperty(key)
    }

    @AfterTest
    fun restoreProps() {
        touchedSysProps.forEach { (k, v) ->
            if (v == null) System.clearProperty(k) else System.setProperty(k, v)
        }
        touchedSysProps.clear()
    }

    @Test
    fun `defaults and environment selection`() =
        testApplication {
            // Ensure no overrides
            clearProp("APP_ENV")
            clearProp("SERVER_PORT")
            clearProp("DB_URL")
            clearProp("DB_USER")
            clearProp("DB_PASSWORD")
            clearProp("DB_DRIVER")
            clearProp("DB_POOL_MAX")
            clearProp("APP_SKIP_DB")
            clearProp("APP_RUN_MIGRATIONS")

            environment {
                config =
                    MapApplicationConfig(
                        // app.env defaults to dev via loader when absent
                    )
            }
            application {
                val cfg = loadAppConfig(this)
                assertEquals("dev", cfg.env)
                assertEquals(DEFAULT_SERVER_PORT, cfg.server.port)
                assertEquals(10, cfg.db.poolMax)
                assertEquals(false, cfg.flags.skipDb)
                assertEquals(true, cfg.flags.runMigrations)
            }
        }

    @Test
    @Disabled("Temporarily disabled; needs to be fixed")
    fun `APP_ENV override to test`() =
        testApplication {
            setProp("APP_ENV", "test")
            environment {
                // no need to set MapApplicationConfig for env; loader will pick APP_ENV
                config = MapApplicationConfig()
            }
            application {
                val cfg = loadAppConfig(this)
                assertEquals("test", cfg.env)
                assertEquals(8080, cfg.server.port)
                assertTrue(cfg.db.url.endsWith("/testdb"), "db.url should end with /testdb: ${cfg.db.url}")
                assertEquals("testuser", cfg.db.user)
                assertEquals(5, cfg.db.poolMax)
            }
        }

    @Test
    fun `environment variable overrides for nested values`() =
        testApplication {
            // We'll use JVM system properties in tests (loader treats them equivalently to env vars)
            setProp("APP_ENV", "dev")
            setProp("DB_POOL_MAX", "42")
            setProp("DB_URL", "jdbc:postgresql://localhost:5432/override")
            setProp("DB_USER", "override_user")
            setProp("DB_PASSWORD", "override_pwd")
            setProp("DB_DRIVER", "org.postgresql.Driver")
            setProp("SERVER_PORT", "9090")

            environment { config = MapApplicationConfig() }
            application {
                val cfg = loadAppConfig(this)
                assertEquals("dev", cfg.env)
                assertEquals(9090, cfg.server.port)
                assertEquals("jdbc:postgresql://localhost:5432/override", cfg.db.url)
                assertEquals("override_user", cfg.db.user)
                assertEquals("override_pwd", cfg.db.password)
                assertEquals("org.postgresql.Driver", cfg.db.driver)
                assertEquals(42, cfg.db.poolMax)
            }
        }

    @Test
    fun `string normalization of env name`() =
        testApplication {
            // Assert that APP_ENV is normalized (trimmed, lowercased)
            setProp("APP_ENV", "PROD")
            environment { config = MapApplicationConfig() }
            application {
                val cfg = loadAppConfig(this)
                assertEquals("prod", cfg.env)
            }
        }

    @Test
    fun `db driver fallback and pool stays zero`() =
        testApplication {
            // Simulate custom env with empty driver and poolMax=0 via in-memory config
            clearProp("APP_ENV")
            environment {
                config =
                    MapApplicationConfig(
                        "app.env" to "custom",
                        "custom.server.port" to DEFAULT_SERVER_PORT.toString(),
                        "custom.db.driver" to "", // empty -> should fallback
                        "custom.db.url" to "jdbc:postgresql://localhost:5432/customdb",
                        "custom.db.user" to "customuser",
                        "custom.db.password" to "custompwd",
                        "custom.db.pool.max" to "0", // stays 0 in AppConfig
                    )
            }
            application {
                val cfg = loadAppConfig(this)
                assertEquals("custom", cfg.env)
                assertEquals("org.postgresql.Driver", cfg.db.driver, "Empty driver should fallback to default")
                assertEquals(
                    0,
                    cfg.db.poolMax,
                    "Config loader should not clamp poolMax; DatabaseFactory enforces later",
                )
            }
        }
}
