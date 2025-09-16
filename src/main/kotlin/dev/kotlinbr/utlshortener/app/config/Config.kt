package dev.kotlinbr.dev.kotlinbr.utlshortener.app.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.util.AttributeKey

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

    // Read environment name only from ApplicationConfig; allow overrides via HOCON substitution (${?APP_ENV})
    val envFromConfig =
        application.environment.config
            .propertyOrNull("app.env")
            ?.getString()
            .normalizedOrNull()

    val env = (envFromConfig ?: "dev")

    // Load the environment-specific section from application.conf (already resolved with substitutions)
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

    fun getBoolean(
        path: String,
        default: Boolean,
    ): Boolean = if (section.hasPath(path)) section.getBoolean(path) else default

    val serverCfg =
        ServerConfig(
            port = getInt("server.port", 8080),
        )

    val dbCfg =
        DbConfig(
            driver = getString("db.driver").ifEmpty { "org.postgresql.Driver" },
            url = getString("db.url"),
            user = getString("db.user"),
            password = getString("db.password"),
            poolMax = getInt("db.pool.max", 10),
        )

    val flags =
        AppFlags(
            skipDb = getBoolean("app.skipDb", false),
            runMigrations = getBoolean("app.runMigrations", true),
        )

    return AppConfig(env = env, server = serverCfg, db = dbCfg, flags = flags)
}
