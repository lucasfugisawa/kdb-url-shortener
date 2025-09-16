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

private fun sysOrEnv(key: String): String? = System.getProperty(key) ?: System.getenv(key)

fun loadAppConfig(application: Application): AppConfig {
    fun String?.normalizedOrNull() = this?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

    // 1) sysprop/env, 2) application.conf's app.env, 3) default
    val envFromSys = sysOrEnv("APP_ENV")?.lowercase()
    val envFromConfig = application.environment.config
        .propertyOrNull("app.env")
        ?.getString()
        ?.normalizedOrNull()

    val env = envFromSys ?: envFromConfig ?: "dev"

    val root: Config = ConfigFactory.parseResources("application.conf").resolve()
    val section: Config = if (root.hasPath(env)) root.getConfig(env) else ConfigFactory.empty()

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

    val serverCfg =
        ServerConfig(
            port = (sysOrEnv("SERVER_PORT"))?.toIntOrNull() ?: getInt("server.port", DEFAULT_SERVER_PORT),
        )

    val dbCfg =
        DbConfig(
            driver =
                sysOrEnv("DB_DRIVER")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getString("db.driver").ifEmpty { "org.postgresql.Driver" },
            url = sysOrEnv("DB_URL")?.trim()?.takeIf { it.isNotEmpty() } ?: getString("db.url"),
            user = sysOrEnv("DB_USER")?.trim()?.takeIf { it.isNotEmpty() } ?: getString("db.user"),
            password = sysOrEnv("DB_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() } ?: getString("db.password"),
            poolMax = sysOrEnv("DB_POOL_MAX")?.toIntOrNull() ?: getInt("db.pool.max", DEFAULT_DB_POOL_MAX),
        )

    val flags =
        AppFlags(
            skipDb =
                sysOrEnv("APP_SKIP_DB")?.trim()?.lowercase()?.let { it == "true" || it == "1" || it == "yes" }
                    ?: getBoolean("app.skipDb", false),
            runMigrations =
                sysOrEnv("APP_RUN_MIGRATIONS")?.trim()?.lowercase()?.let { it == "true" || it == "1" || it == "yes" }
                    ?: getBoolean("app.runMigrations", true),
        )

    return AppConfig(env = env, server = serverCfg, db = dbCfg, flags = flags)
}
