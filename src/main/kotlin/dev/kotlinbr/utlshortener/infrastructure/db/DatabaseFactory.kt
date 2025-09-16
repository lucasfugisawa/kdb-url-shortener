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

    @Volatile
    private var currentSignature: String? = null

    // Testing-visible accessor and reset to support integration tests
    internal fun getDataSourceForTesting(): HikariDataSource? = dataSource

    internal fun resetForTesting() {
        try {
            dataSource?.close()
        } catch (_: Exception) {
            // ignore
        } finally {
            dataSource = null
            currentSignature = null
        }
    }

    fun init(config: AppConfig) {
        synchronized(this) {
            val newSignature = signatureFor(config)
            val ds = dataSource
            if (ds != null) {
                val sameConfig = (currentSignature == newSignature)
                val valid =
                    try {
                        // Try to validate by getting a connection quickly
                        ds.connection.use { true }
                    } catch (_: Exception) {
                        false
                    }
                if (sameConfig && valid) {
                    return
                }
                // Config changed or datasource invalid: rebuild
                try {
                    ds.close()
                } catch (_: Exception) {
                    // ignore
                }
                dataSource = null
            }

            val created = hikari(config)
            dataSource = created
            currentSignature = newSignature
            Database.connect(created)
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED

            if (shouldRunMigrations(config)) {
                runMigrations(created)
            }
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

    private fun signatureFor(config: AppConfig): String {
        val db = config.db
        return listOf(db.url, db.user, db.password, db.poolMax.toString(), db.driver).joinToString("|")
    }

    private fun hikari(config: AppConfig): HikariDataSource {
        val db = config.db
        val cfg =
            HikariConfig().apply {
                jdbcUrl = db.url
                username = db.user
                password = db.password
                maximumPoolSize = if (db.poolMax > 0) db.poolMax else DEFAULT_DB_POOL_MAX
                // Fallback driver if blank
                driverClassName = if (db.driver.isBlank()) "org.postgresql.Driver" else db.driver
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
