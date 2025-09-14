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
 )

 data class AppConfig(
     val env: String,
     val db: DbConfig,
 )

 val AppConfigKey: AttributeKey<AppConfig> = AttributeKey("AppConfig")

 fun loadAppConfig(application: Application): AppConfig {
     // Determine environment: APP_ENV env var -> application config property -> default "dev"
     val envFromEnv = System.getenv("APP_ENV")
     val envFromConfig = application.environment.config.propertyOrNull("app.env")?.getString()
     val env = (envFromEnv ?: envFromConfig ?: "dev").lowercase()

     // Parse application.conf and pick the matching section if present
     val root: Config = ConfigFactory.parseResources("application.conf").resolve()
     val section: Config = if (root.hasPath(env)) root.getConfig(env) else ConfigFactory.empty()

     fun get(path: String, default: String = ""): String =
         if (section.hasPath(path)) section.getString(path) else default

     val dbCfg = DbConfig(
         driver = get("db.driver"),
         url = get("db.url"),
         user = get("db.user"),
         password = get("db.password"),
     )

     return AppConfig(env = env, db = dbCfg)
 }
