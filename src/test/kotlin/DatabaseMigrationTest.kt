package dev.kotlinbr

import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationTest {
    private lateinit var pg: PostgreSQLContainer<*>

    @BeforeTest
    fun setUp() {
        pg = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
        pg.start()

        // Configure application to use container DB and run migrations
        System.setProperty("APP_ENV", "test")
        System.setProperty("DB_URL", pg.jdbcUrl)
        System.setProperty("DB_USER", pg.username)
        System.setProperty("DB_PASSWORD", pg.password)
        System.setProperty("DB_POOL_MAX", "5")
        System.setProperty("APP_RUN_MIGRATIONS", "true")
    }

    @AfterTest
    fun tearDown() {
        try {
            pg.stop()
        } catch (_: Throwable) {
        }
        System.clearProperty("APP_ENV")
        System.clearProperty("DB_URL")
        System.clearProperty("DB_USER")
        System.clearProperty("DB_PASSWORD")
        System.clearProperty("DB_POOL_MAX")
        System.clearProperty("APP_RUN_MIGRATIONS")
    }

    @Test
    fun `migration runs on startup and creates hello table`() =
        testApplication {
            application { module() }

            // Ensure readiness endpoint reports ready
            val resp = client.get("/health/ready")
            kotlin.test.assertEquals(200, resp.status.value)

            // Verify table exists by querying it via JDBC
            DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery("select count(*) from hello").use { rs ->
                        rs.next()
                        val count = rs.getInt(1)
                        // If table exists, count is an integer (possibly 0)
                        assertEquals(true, count >= 0)
                    }
                }
            }
        }
}
