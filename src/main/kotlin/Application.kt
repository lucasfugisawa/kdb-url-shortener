package dev.kotlinbr

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Load app configuration based on environment
    val appConfig = loadAppConfig(this)
    this.attributes.put(AppConfigKey, appConfig)
    // Log selected environment at startup
    this.environment.log.info("Application starting with env=${appConfig.env}")

    val skipDb = System.getProperty("APP_SKIP_DB") == "true"
    if (!skipDb) {
        // Initialize database (Hikari + Exposed) and optionally run migrations with Flyway
        DatabaseFactory.init(appConfig)
        this.environment.log.info("Database initialized")
    } else {
        this.environment.log.info("Skipping database initialization due to APP_SKIP_DB=true")
    }

    configureHTTP()
    configureSerialization()
    configureRouting()
}
