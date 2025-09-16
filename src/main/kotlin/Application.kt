package dev.kotlinbr

import dev.kotlinbr.utlshortener.app.config.AppConfigKey
import dev.kotlinbr.utlshortener.app.config.loadAppConfig
import dev.kotlinbr.utlshortener.app.http.configureHTTP
import dev.kotlinbr.utlshortener.infrastructure.db.DatabaseFactory
import dev.kotlinbr.utlshortener.interfaces.http.configureRouting
import dev.kotlinbr.utlshortener.interfaces.http.configureSerialization
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    val appConfig =
        loadAppConfig(this)
    this.attributes.put(AppConfigKey, appConfig)
    this.environment.log.info("Application starting with env=${appConfig.env}")

    if (!appConfig.flags.skipDb) {
        DatabaseFactory.init(appConfig)
        this.environment.log.info("Database initialized")
    } else {
        this.environment.log.info("Skipping database initialization due to app.skipDb=true")
    }

    configureHTTP()
    configureSerialization()
    configureRouting()
}
