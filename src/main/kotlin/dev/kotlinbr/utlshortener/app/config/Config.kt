package dev.kotlinbr.dev.kotlinbr.utlshortener.app.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

const val DEFAULT_SERVER_PORT: Int = 8080
const val DEFAULT_DB_POOL_MAX: Int = 10

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

data class AppFlags(
    val skipDb: Boolean,
    val runMigrations: Boolean,
)

data class AppConfig(
    val env: String,
    val server: ServerConfig,
    val db: DbConfig,
    val flags: AppFlags,
)

val AppConfigKey: AttributeKey<AppConfig> = AttributeKey("AppConfig")

fun loadAppConfig(application: Application): AppConfig {
    fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

    // Determine environment: prefer JVM sysprop/env, then ApplicationConfig, default to "dev"
    val envFromConfig =
        application.environment.config
            .propertyOrNull("app.env")
            ?.getString()
            ?.normalizedOrNull()
    val envFromSys = System.getProperty("APP_ENV").normalizedOrNull()

    val env = (envFromConfig ?: envFromSys ?: "dev")

    // Load the environment-specific section from the full merged configuration on the classpath.
    // Using ConfigFactory.load() ensures resources, system properties, and reference.conf are considered,
    // which is more robust across different environments/CI.
    val root: Config = ConfigFactory.load()
    val section: Config = if (root.hasPath(env)) root.getConfig(env) else ConfigFactory.empty()

    // Helpers that first check ApplicationConfig under the resolved env section (for in-memory overrides),
    // then fall back to the resource section, then defaults.
    fun getString(
        path: String,
        default: String = "",
    ): String {
        val fromApp =
            application.environment.config
                .propertyOrNull("$env.$path")
                ?.getString()
        return fromApp ?: if (section.hasPath(path)) section.getString(path) else default
    }

    fun getInt(
        path: String,
        default: Int,
    ): Int {
        val fromApp =
            application.environment.config
                .propertyOrNull("$env.$path")
                ?.getString()
                ?.toIntOrNull()
        return fromApp ?: if (section.hasPath(path)) section.getInt(path) else default
    }

    fun getBoolean(
        path: String,
        default: Boolean,
    ): Boolean {
        val fromApp =
            application.environment.config
                .propertyOrNull("$env.$path")
                ?.getString()
                ?.trim()
                ?.lowercase()
        val normalized =
            when (fromApp) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> null
            }
        return normalized ?: if (section.hasPath(path)) section.getBoolean(path) else default
    }

    // Top-level app flags may be placed inside the env section as in our application.conf.

    val serverCfg =
        ServerConfig(
            port =
                System.getProperty("SERVER_PORT")?.toIntOrNull()
                    ?: getInt("server.port", DEFAULT_SERVER_PORT),
        )

    val dbCfg =
        DbConfig(
            driver =
                System.getProperty("DB_DRIVER")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString("db.driver").ifEmpty { "org.postgresql.Driver" },
            url =
                System.getProperty("DB_URL")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString("db.url"),
            user =
                System.getProperty("DB_USER")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString("db.user"),
            password =
                System.getProperty("DB_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString("db.password"),
            poolMax =
                System.getProperty("DB_POOL_MAX")?.toIntOrNull()
                    ?: getInt("db.pool.max", DEFAULT_DB_POOL_MAX),
        )

    val flags =
        AppFlags(
            skipDb =
                System
                    .getProperty("APP_SKIP_DB")
                    ?.trim()
                    ?.lowercase()
                    ?.let { it == "true" || it == "1" || it == "yes" }
                    ?: getBoolean("app.skipDb", false),
            runMigrations =
                System
                    .getProperty("APP_RUN_MIGRATIONS")
                    ?.trim()
                    ?.lowercase()
                    ?.let { it == "true" || it == "1" || it == "yes" }
                    ?: getBoolean("app.runMigrations", true),
        )

    return AppConfig(env = env, server = serverCfg, db = dbCfg, flags = flags)
}
