package dev.kotlinbr.dev.kotlinbr.utlshortener.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.AppConfig
import dev.kotlinbr.dev.kotlinbr.utlshortener.app.config.DEFAULT_DB_POOL_MAX
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

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

    private fun shouldRunMigrations(config: AppConfig): Boolean = config.flags.runMigrations

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
                maximumPoolSize = if (db.poolMax > 0) db.poolMax else DEFAULT_DB_POOL_MAX
                driverClassName = db.driver
                validate()
            }
        return HikariDataSource(cfg)
    }

    fun isHealthy(): Boolean {
        dataSource ?: return false
        return try {
            transaction {
                exec("SELECT 1") { rs ->
                    if (rs.next()) rs.getInt(1) == 1 else false
                } ?: false
            }
        } catch (e: SQLException) {
            logger.debug("DB health check failed due to SQL exception", e)
            false
        } catch (e: IllegalStateException) {
            logger.debug("DB health check failed due to illegal state", e)
            false
        }
    }
}
