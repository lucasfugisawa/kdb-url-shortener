package dev.kotlinbr.utlshortener.testutils

import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppFlags
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DbConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.ServerConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for integration tests that need a PostgreSQL database.
 * - Uses a reusable Testcontainers PostgreSQL container (shared per class).
 * - Exposes JDBC URL/credentials helpers.
 * - Provides helpers to build AppConfig and initialize DatabaseFactory.
 * - Sets APP_ENV=test and APP_RUN_MIGRATIONS=true system properties for tests.
 */
@Tag("integration")
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
open class BaseIntegrationTest {
    protected val logger: Logger = LoggerFactory.getLogger(BaseIntegrationTest::class.java)

    companion object {
        @JvmStatic
        @Container
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16-alpine").apply {
                withReuse(true)
            }
    }

    @BeforeAll
    fun beforeAll() {
        // Ensure container is running
        if (!postgres.isRunning) {
            postgres.start()
        }
        // Provide commonly used system properties for the app/tests
        System.setProperty("APP_ENV", "test")
        System.setProperty("APP_RUN_MIGRATIONS", "true")
        System.setProperty("DB_URL", jdbcUrl())
        System.setProperty("DB_USER", username())
        System.setProperty("DB_PASSWORD", password())
    }

    @AfterAll
    fun afterAll() {
        // Do not stop container explicitly when reuse=true; let Testcontainers manage lifecycle
    }

    protected fun jdbcUrl(): String = postgres.jdbcUrl

    protected fun username(): String = postgres.username

    protected fun password(): String = postgres.password

    protected fun createAppConfig(runMigrations: Boolean = true): AppConfig =
        AppConfig(
            env = "test",
            server = ServerConfig(port = 0),
            db =
                DbConfig(
                    driver = "org.postgresql.Driver",
                    url = jdbcUrl(),
                    user = username(),
                    password = password(),
                    poolMax = 5,
                ),
            flags = AppFlags(skipDb = false, runMigrations = runMigrations),
        )

    protected fun initDatabase(runMigrations: Boolean = true): AppConfig {
        val cfg = createAppConfig(runMigrations)
        DatabaseFactory.init(cfg)
        return cfg
    }
}
