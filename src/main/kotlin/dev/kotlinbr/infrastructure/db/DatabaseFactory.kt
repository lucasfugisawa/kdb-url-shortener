package dev.kotlinbr.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kotlinbr.app.config.AppConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object DatabaseFactory {
    private val logger = org.slf4j.LoggerFactory.getLogger(DatabaseFactory::class.java)

    @Volatile
    private var dataSource: HikariDataSource? = null

    fun init(config: AppConfig) {
        if (dataSource != null) return

        val ds = hikari(config)
        dataSource = ds
        Database.connect(ds)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED

        if (shouldRunMigrations(config)) {
            runMigrations(ds)
        }
    }

    private fun shouldRunMigrations(config: AppConfig): Boolean {
        val env = System.getenv("APP_RUN_MIGRATIONS")?.lowercase()
        val prop = System.getProperty("APP_RUN_MIGRATIONS")?.lowercase()
        return env == "true" || prop == "true" || config.env == "test"
    }

    private fun runMigrations(ds: HikariDataSource) {
        val flyway =
            Flyway
                .configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
        flyway.migrate()
    }

    private fun hikari(config: AppConfig): HikariDataSource {
        val db = config.db
        val cfg =
            HikariConfig().apply {
                jdbcUrl = db.url
                username = db.user
                password = db.password
                maximumPoolSize = if (db.poolMax > 0) db.poolMax else 10
                driverClassName = db.driver
                validate()
            }
        return HikariDataSource(cfg)
    }

    fun isHealthy(): Boolean {
        val ds = dataSource ?: return false
        return try {
            transaction {
                exec("SELECT 1") { rs ->
                    if (rs.next()) rs.getInt(1) == 1 else false
                } ?: false
            }
        } catch (e: java.sql.SQLException) {
            logger.debug("DB health check failed due to SQL exception", e)
            false
        } catch (e: IllegalStateException) {
            logger.debug("DB health check failed due to illegal state", e)
            false
        }
    }
}
