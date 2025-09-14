package dev.kotlinbr

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

/**
 * Application configuration models
 */
data class DbConfig(
    val driver: String,
    val url: String,
    val user: String,
    val password: String,
    val poolMax: Int,
)

data class ServerConfig(
    val port: Int,
)

data class AppConfig(
    val env: String,
    val server: ServerConfig,
    val db: DbConfig,
)

val AppConfigKey: AttributeKey<AppConfig> = AttributeKey("AppConfig")

fun loadAppConfig(application: Application): AppConfig {
    // Determine environment: APP_ENV env var -> system property (for tests) -> application config property -> default "dev"
    val envFromEnv = System.getenv("APP_ENV")
    val envFromSysProp = System.getProperty("APP_ENV")
    val envFromConfig = application.environment.config.propertyOrNull("app.env")?.getString()
    val env = (envFromEnv ?: envFromSysProp ?: envFromConfig ?: "dev").lowercase()

    // Parse application.conf and pick the matching section if present
    val root: Config = ConfigFactory.parseResources("application.conf").resolve()
    val section: Config = if (root.hasPath(env)) root.getConfig(env) else ConfigFactory.empty()

    fun getString(
        path: String,
        default: String = "",
    ): String = if (section.hasPath(path)) section.getString(path) else default

    fun getInt(
        path: String,
        default: Int,
    ): Int = if (section.hasPath(path)) section.getInt(path) else default

    val serverCfg =
        ServerConfig(
            port = getInt("server.port", 8080),
        )

    // Allow overrides via environment variables or system properties for tests/runtimes
    fun getEnvOrProp(name: String): String? = System.getenv(name) ?: System.getProperty(name)

    val dbCfg =
        DbConfig(
            driver = getString("db.driver").ifEmpty { getEnvOrProp("DB_DRIVER") ?: "org.postgresql.Driver" },
            url = getEnvOrProp("DB_URL") ?: getString("db.url"),
            user = getEnvOrProp("DB_USER") ?: getString("db.user"),
            password = getEnvOrProp("DB_PASSWORD") ?: getString("db.password"),
            poolMax = (getEnvOrProp("DB_POOL_MAX")?.toIntOrNull()) ?: getInt("db.pool.max", 10),
        )

    return AppConfig(env = env, server = serverCfg, db = dbCfg)
}
